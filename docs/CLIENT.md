# Client
This markdown covers the topic of custom Spotify Client credentials - mainly used for interaction with Spotify Web API.
These credentials are used for "Account login" in Outify settings.

No default credentials ship with the app. You must supply your own before Web API features work.

### Why use custom credentials?
Outify is built with client credentials shared amongst all other users. Spotify can easily see all this traffic and might prohibit you from using their API via the default credentials.
By using custom credentials you should no longer be easy to get flagged and limited in any way.

### Why are these credentials required?
Outify uses Spotify Web API to interact with your profile.
- liking/unliking tracks, playlists, artists, ..
- searching
- viewing user profiles
- ..

Without these credentials you are unable to use these features.

### Using custom credentials
I. Visit [Spotify Developer's Dashboard](https://developer.spotify.com/dashboard). (You have to be logged in)
II. Click "Create app".
III. Fill in the required fields and fill out these fields as said:
- __Redirect URIs__: http://127.0.0.1:5588/account/login | This is required for OAuth to complete automatically
- __Which API/SDKs are you planning to use?__: Web API | This is required for already mentioned features to work correctly.
IV. Copy your __Client ID__ and __Client secret__ (need to click on "View client secret").
V. Open Outify. Navigate to settings > Playback > Scroll down to Advanced Settings (click) > paste your client id and secret respectively.
VI. Restart Outify for your credentials to take effect.
