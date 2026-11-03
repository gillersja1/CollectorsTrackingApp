// The security rules, checked against the Firestore emulator: each user can reach their own collection and
// photos, nobody else's, and nothing outside users/{uid}.
// Run with `npm test` in this folder (it starts the emulators); see firebase/README.md.
import { readFileSync } from 'node:fs';
import { after, before, beforeEach, describe, test } from 'node:test';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { deleteDoc, doc, getDoc, setDoc } from 'firebase/firestore';
import { Bytes } from 'firebase/firestore';

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-collectors',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8'), host: '127.0.0.1', port: 8080 },
  });
});

after(async () => {
  await env?.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
});

const ada = () => env.authenticatedContext('ada');
const grace = () => env.authenticatedContext('grace');
const nobody = () => env.unauthenticatedContext();

const item = (cloudId, extra = {}) => ({
  cloudId,
  name: 'Batman',
  category: 'FunkoPop',
  description: 'First edition',
  imagePath: '',
  timestamp: 1,
  isWishlist: false,
  estimatedValue: 12.5,
  tags: 'dc',
  ...extra,
});

/** Puts a document in place without going through the rules. */
async function seed(path, data) {
  await env.withSecurityRulesDisabled(async (admin) => {
    await setDoc(doc(admin.firestore(), path), data);
  });
}

describe('Firestore: a collection belongs to its owner', () => {
  test('a user can save, read and delete their own items', async () => {
    const db = ada().firestore();
    const batman = doc(db, 'users/ada/collectibles/abc-123');

    await assertSucceeds(setDoc(batman, item('abc-123')));
    await assertSucceeds(getDoc(batman));
    await assertSucceeds(deleteDoc(batman));
  });

  test("a user can't read someone else's items", async () => {
    await seed('users/ada/collectibles/abc-123', item('abc-123'));

    await assertFails(getDoc(doc(grace().firestore(), 'users/ada/collectibles/abc-123')));
  });

  test("a user can't write or delete someone else's items", async () => {
    await seed('users/ada/collectibles/abc-123', item('abc-123'));
    const theirs = doc(grace().firestore(), 'users/ada/collectibles/abc-123');

    await assertFails(setDoc(theirs, item('abc-123', { name: 'Defaced' })));
    await assertFails(deleteDoc(theirs));
    await assertFails(setDoc(doc(grace().firestore(), 'users/ada/collectibles/new-one'), item('new-one')));
  });

  test('signed-out visitors get nothing', async () => {
    await seed('users/ada/collectibles/abc-123', item('abc-123'));
    const db = nobody().firestore();

    await assertFails(getDoc(doc(db, 'users/ada/collectibles/abc-123')));
    await assertFails(setDoc(doc(db, 'users/ada/collectibles/x'), item('x')));
  });

  test('nothing outside users/{uid} is open', async () => {
    await assertFails(setDoc(doc(ada().firestore(), 'everyone/notes'), { text: 'hi' }));
    await assertFails(getDoc(doc(ada().firestore(), 'everyone/notes')));
  });

  test('an item has to claim its own cloud id', async () => {
    await assertFails(setDoc(doc(ada().firestore(), 'users/ada/collectibles/abc-123'), item('something-else')));
  });

  test('items from builds before cloud ids (no cloudId field) are still accepted', async () => {
    const { cloudId, ...old } = item('5');
    await assertSucceeds(setDoc(doc(ada().firestore(), 'users/ada/collectibles/5'), old));
  });

  test('an item has to have the right shape', async () => {
    const db = ada().firestore();
    await assertFails(setDoc(doc(db, 'users/ada/collectibles/a'), item('a', { name: 42 })));
    await assertFails(setDoc(doc(db, 'users/ada/collectibles/b'), item('b', { name: 'x'.repeat(201) })));
    await assertFails(setDoc(doc(db, 'users/ada/collectibles/c'), item('c', { isWishlist: 'yes' })));
    const { name, ...nameless } = item('d');
    await assertFails(setDoc(doc(db, 'users/ada/collectibles/d'), nameless));
  });

  test('categories: own only, named after their document', async () => {
    await assertSucceeds(setDoc(doc(ada().firestore(), 'users/ada/categories/Comics'), { id: 0, name: 'Comics' }));
    await assertFails(setDoc(doc(ada().firestore(), 'users/ada/categories/Comics'), { id: 0, name: 'Cards' }));
    await assertFails(setDoc(doc(grace().firestore(), 'users/ada/categories/Comics'), { id: 0, name: 'Comics' }));
  });

  test('a user can delete their own account document (account deletion)', async () => {
    await seed('users/ada', { created: 1 });

    await assertFails(deleteDoc(doc(grace().firestore(), 'users/ada')));
    await assertSucceeds(deleteDoc(doc(ada().firestore(), 'users/ada')));
  });
});

describe('Firestore: photos belong to their owner', () => {
  const photo = (bytes = 2000, extra = {}) => ({
    data: Bytes.fromUint8Array(new Uint8Array(bytes)),
    contentType: 'image/jpeg',
    createdAt: 1,
    ...extra,
  });

  test('a user can save, read and delete their own photos', async () => {
    const mine = doc(ada().firestore(), 'users/ada/photos/p1');

    await assertSucceeds(setDoc(mine, photo()));
    await assertSucceeds(getDoc(mine));
    await assertSucceeds(deleteDoc(mine));
  });

  test("a user can't read, overwrite or delete someone else's photos", async () => {
    await seed('users/ada/photos/p1', photo());
    const theirs = doc(grace().firestore(), 'users/ada/photos/p1');

    await assertFails(getDoc(theirs));
    await assertFails(setDoc(theirs, photo()));
    await assertFails(deleteDoc(theirs));
  });

  test('signed-out visitors get nothing', async () => {
    await seed('users/ada/photos/p1', photo());

    await assertFails(getDoc(doc(nobody().firestore(), 'users/ada/photos/p1')));
  });

  test('only JPEG bytes, and nothing over 900 KB', async () => {
    const db = ada().firestore();
    await assertFails(setDoc(doc(db, 'users/ada/photos/big'), photo(900 * 1024 + 1)));
    await assertFails(setDoc(doc(db, 'users/ada/photos/png'), photo(2000, { contentType: 'image/png' })));
    await assertFails(setDoc(doc(db, 'users/ada/photos/text'), photo(2000, { data: 'not bytes' })));
    await assertSucceeds(setDoc(doc(db, 'users/ada/photos/ok'), photo(700_000)));
  });
});
