# Security rules

The Firestore rules for the app, kept here so they're reviewed and tested like code.
Each user's data lives under `users/{uid}`: items (`collectibles`), `categories`, and `photos`.
Only that signed-in user can read, change or delete it, and everything else is closed.
Writes are also checked for shape and size: names up to 200 characters, photos JPEG only and at most 900 KB.

**Photos are stored in Firestore**, not Firebase Storage. Storage needs a billing account, and Firestore's free
plan already includes 1 GB, which holds thousands of photos. The app shrinks each photo to at most 1024 pixels
and 700 KB before uploading it, to fit Firestore's 1 MiB limit per document.

## Test them

The tests run against the Firestore emulator, never the real project (they use the `demo-collectors` project id).
You need Node and Java 11 or later.

```bash
cd firebase
npm ci
npm test
```

CI runs them on every push and pull request (the `security-rules` job).

## Deploy them

Deploying replaces the rules in the Firebase console:

```bash
cd firebase
npx firebase login
npx firebase deploy --only firestore:rules --project collectorsapp-90a46 --config ../firebase.json
```
