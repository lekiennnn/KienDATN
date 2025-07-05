## 📸 Cloudinary Image Upload Setup Instructions

1. Log in to your Cloudinary account at https://cloudinary.com/
2. Go to Settings → Upload
3. Click "Add upload preset"
4. Enter exactly "mobile_upload" as the name (must match the app's code)
5. Set Signing Mode to "Unsigned"
6. Optionally set folder to "app_uploads"
7. Click Save

That's it! Your app is already configured with your Cloudinary account.
When you post images, they'll be uploaded to Cloudinary instead of Firebase Storage.