# Setting Up Cloudinary in Your App

This guide will help you configure and use Cloudinary for image uploads in your Android app.

## 1. Create a Cloudinary Account

1. Go to [Cloudinary's website](https://cloudinary.com/) and sign up for a free account
2. After registration, you will see your dashboard with your Cloud Name, API Key, and API Secret

## 2. Configure Your Cloudinary Upload Preset

1. In the Cloudinary dashboard, go to **Settings** → **Upload**
2. Scroll down to **Upload presets** section and click **Add upload preset**
3. Give your preset a name (e.g., `mobile_upload`)
4. Set **Signing Mode** to **Unsigned** (for client-side uploading)
5. Configure other settings as needed:
   - **Folder**: Set a default folder for uploads (e.g., `app_uploads`)
   - **Access Mode**: Public or Private
   - **Resource Type**: Set to **Auto** for best results
6. Click **Save** to create the upload preset

## 3. Update Your App Configuration

Open `MyApplication.kt` and update these constants with your Cloudinary credentials:

```kotlin
// Cloudinary credentials - replace with your values
private const val CLOUDINARY_CLOUD_NAME = "your_cloud_name" // Replace with your cloud name
private const val CLOUDINARY_UPLOAD_PRESET = "mobile_upload" // Replace with your upload preset name
```

## 4. How Cloudinary Works in This App

- When you create a post or add a comment with an image, the `CloudinaryUploader` class handles the image upload
- The image is sent directly from your device to Cloudinary's servers
- Once the upload is successful, Cloudinary returns a URL to the uploaded image
- This URL is then stored in your Firebase database as part of the post or comment

## 5. Image Transformations

Cloudinary allows you to transform images on-the-fly by modifying the URL. For example:

- Resize an image to 300x300 pixels:
  `https://res.cloudinary.com/your-cloud-name/image/upload/w_300,h_300/v1234567890/image-id.jpg`

- Crop an image to focus on faces:
  `https://res.cloudinary.com/your-cloud-name/image/upload/c_face,g_face/v1234567890/image-id.jpg`

Learn more about image transformations in the [Cloudinary documentation](https://cloudinary.com/documentation/image_transformations).

## Troubleshooting

- If images fail to upload, check your internet connection and verify your Cloudinary credentials
- Make sure your upload preset exists and is set to "unsigned"
- Check Logcat for detailed error messages from the `CloudinaryUploader` class