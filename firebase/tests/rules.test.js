// The security rules, checked against the Firestore and Storage emulators: each user can reach their own
// collection and photos, nobody else's, and nothing outside users/{uid}.
// Run with `npm test` in this folder (it starts the emulators); see firebase/README.md.
import { readFileSync } from 'node:fs';
import { after, before, beforeEach, describe, test } from 'node:test';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { deleteDoc, doc, getDoc, setDoc } from 'firebase/firestore';
import { deleteObject, getBytes, ref, uploadBytes } from 'firebase/storage';

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-collectors',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8'), host: '127.0.0.1', port: 8080 },
    storage: { rules: readFileSync(new URL('../storage.rules', import.meta.url), 'utf8'), host: '127.0.0.1', port: 9199 },
  });
});

after(async () => {
  await env?.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
  await env.clearStorage();
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

describe('Storage: photos belong to their owner', () => {
  const jpeg = new Uint8Array([0xff, 0xd8, 0xff, 0xe0]);

  test('a user can upload, read and delete their own photos', async () => {
    const photo = ref(ada().storage(), 'users/ada/images/collectibles_1.jpg');

    await assertSucceeds(uploadBytes(photo, jpeg, { contentType: 'image/jpeg' }));
    await assertSucceeds(getBytes(photo));
    await assertSucceeds(deleteObject(photo));
  });

  test("a user can't read, overwrite or delete someone else's photos", async () => {
    await assertSucceeds(uploadBytes(ref(ada().storage(), 'users/ada/images/p.jpg'), jpeg, { contentType: 'image/jpeg' }));
    const theirs = ref(grace().storage(), 'users/ada/images/p.jpg');

    await assertFails(getBytes(theirs));
    await assertFails(uploadBytes(theirs, jpeg, { contentType: 'image/jpeg' }));
    await assertFails(deleteObject(theirs));
  });

  test('signed-out visitors and other paths are closed', async () => {
    await assertFails(uploadBytes(ref(nobody().storage(), 'users/ada/images/p.jpg'), jpeg, { contentType: 'image/jpeg' }));
    await assertFails(uploadBytes(ref(ada().storage(), 'public/p.jpg'), jpeg, { contentType: 'image/jpeg' }));
  });

  test('only images, and nothing over 10 MB', async () => {
    const storage = ada().storage();
    await assertFails(uploadBytes(ref(storage, 'users/ada/images/notes.txt'), new Uint8Array([1, 2, 3]), { contentType: 'text/plain' }));
    await assertFails(uploadBytes(ref(storage, 'users/ada/images/huge.jpg'), new Uint8Array(10 * 1024 * 1024 + 1), { contentType: 'image/jpeg' }));
  });
});
