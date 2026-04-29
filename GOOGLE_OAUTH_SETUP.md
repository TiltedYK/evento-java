# Google Sign-In setup (one-time, ~3 minutes)

The login screen has a **"Continue with Google"** button that opens the real
Google account chooser in the user's default browser, then auto-creates the
account in your local database the first time someone signs in with it.

To make it work you need a Google OAuth Client ID. It's free.

## 1. Create an OAuth client

1. Go to <https://console.cloud.google.com/>.
2. Pick (or create) a project — anything named e.g. *EVENTO*.
3. In the left menu: **APIs & Services → OAuth consent screen**.
   - User type: **External**.
   - App name: `EVENTO`. Support email: yours. Developer email: yours. Save.
   - On the next pages just click **Save and continue** until you're back to
     the dashboard. You don't need any scopes; the basic profile/email
     scopes are granted automatically for OAuth.
   - While the app is "Testing", add yourself (and any teammates) as
     **Test users**.
4. Left menu: **APIs & Services → Credentials → + Create credentials →
   OAuth client ID**.
   - Application type: **Desktop app**.
   - Name: `EVENTO Desktop`.
   - Click **Create**.
5. A dialog appears with **Client ID** and **Client secret** — copy them.

## 2. Drop them into the app

You have **three** ways to give the app the credentials, and any of them
works (the app picks the highest-priority one available):

### Option A — paste them in the in-app dialog (easiest)

1. Run the app and click **"Continue with Google"** on the login screen.
2. A dialog pops up asking for Client ID + Client Secret.
3. Paste both, click **Save & Continue** — the OAuth flow starts immediately.

The credentials are persisted to `~/.evento/google_oauth.properties` so
you only need to do this once per machine.

### Option B — edit the resource file (for committed defaults)

Open `src/main/resources/google_oauth.properties` and replace the two
placeholder lines:

```
google.client.id=123456789-abcdef.apps.googleusercontent.com
google.client.secret=GOCSPX-xxxxxxxxxxxxxxxxxxxx
```

### Option C — JVM system properties (for CI / one-off testing)

Add these to your run configuration's VM options:

```
-Dgoogle.client.id=...
-Dgoogle.client.secret=...
```

System properties win over both files above.

> ℹ️ With a "Desktop app" client, Google accepts any redirect on
> `http://127.0.0.1:<random-port>`, which is what `GoogleAuthService` spins
> up at runtime — no need to register a redirect URI by hand.

## 3. What happens on first login

1. The app opens your default browser to the Google account chooser.
2. You pick an account → Google redirects back to a one-shot localhost
   server inside the JavaFX app.
3. The app exchanges the code for an access token and reads
   `email`, `given_name`, `family_name`, `picture`, `sub` from
   `openidconnect.googleapis.com/v1/userinfo`.
4. It looks up the user by email in the `user` table:
   - **Found** → logs in (routes to admin or front dashboard by role).
   - **Not found** → inserts a new row with role `ROLE_USER`,
     `password = "__GOOGLE_OAUTH__"` (a placeholder so the row is unique;
     the user can never log in via password until they pick one),
     and signs in.

## Security notes

- Never commit `google_oauth.properties` with real values to a public repo.
  Add it to `.gitignore` if you fork.
- The client secret in a desktop app is **not** a secret in the
  cryptographic sense — Google explicitly designs the desktop OAuth flow
  knowing it ships in clients. The `state` parameter prevents CSRF and the
  loopback redirect prevents code interception by other apps.
- If you want to revoke access from a Google account: <https://myaccount.google.com/permissions>.

## Verifying you're running the latest code

The new code prints a build tag to the **Run** console the moment the
login screen appears. Look for this line:

```
[EVENTO] LoginController loaded — build=EVENTO_LOGIN_BUILD_2026_04_28_GOOGLE_OAUTH_V1  gauthBuild=EVENTO_GAUTH_BUILD_2026_04_28_OAUTH_LOOPBACK  creds=...
```

If you don't see it, the JVM is running stale `.class` files —
**Build → Rebuild Project** in IntelliJ, or run `mvn clean compile`.
