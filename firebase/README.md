# Security rules

Firestore and Storage rules for the app, kept here so they're reviewed and tested like code.
Each user's data lives under `users/{uid}` (items, categories) and `users/{uid}/images` (photos);
only that signed-in user can read, change or delete it, and everything else is closed.
Writes are also checked for shape and size: names up to 200 characters, photos up to 10 MB and images only.

## Test them

The tests run against the Firebase emulators, never the real project (they use the `demo-collectors` project id).
You need Node and Java 11 or later.

```bash
cd firebase
npm ci
npm test
```

CI runs them on every push and pull request (the `security-rules` job).

## Deploy them

Deploying replaces the rules in the Firebase console, so compare them with what's there first:

```bash
cd firebase
npx firebase login
npx firebase deploy --only firestore:rules,storage --project collectorsapp-90a46 --config ../firebase.json
```
