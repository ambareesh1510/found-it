# Release Notes: GT Lost and Found v1.0

**Project Name:** GT Lost and Found

**Team Name:** DCD-6137

**Client:** Ann McClellan

**Release Version:** 1.0  

**Release Date:** April 28, 2026

---

## 1. New Software Features (v1.0)
This is the initial release of the GT Lost and Found platform. The following primary features have been developed to support the Georgia Tech community in recovering lost items.

### User Accounts & Authentication
* **Registration & Login**: Secure account creation and authentication to protect user data.
* **Google OAuth**: Fast sign-in using Google accounts without requiring a new password.
* **Logout Functionality**: Ability to invalidate sessions to protect accounts on shared devices.

### Item Management
* **Item Posting**: Users can create posts with specific item details.
* **Image Uploads**: Support for uploading and previewing images for visual recognition.
* **Browse & Search**: List and grid layouts to browse all reported items.
* **Detail Pages**: Dedicated views to see in-depth information about a specific item.
* **Resolution Tracking**: Users can mark items as resolved once they have been returned.

### Campus Map Integration
* **Campus Map View**: Integration with Google Maps to display lost items geographically.
* **Interactive Markers**: Clickable map markers that display item details in a popup.
* **Distance-Based Filtering**: Ability to filter items based on proximity to the user's current location.
* **Map Picker**: A coordinate selection tool used when creating new posts for high location accuracy.

### Real-Time Communication
* **WebSocket Messaging**: Real-time messaging system to coordinate item returns.
* **Conversation History**: Dashboard to view and manage all ongoing discussions.

### Profile Personalization
* **Profile Management**: Editable user profiles to keep contact information accurate.

### Star Feature: AI-Powered Item Recognition
* **Automatic Category Detection**: Uses computer vision to analyze photos and automatically suggest item categories such as wallets, keys, or laptops.
* **User Control**: Allows users to confirm or edit the AI-predicted category before finalizing the post to maintain classification accuracy.

---

## 2. Bug Fixes
The following issues were identified and resolved during the development of Version 1.0:

* **Authentication Validation**: Corrected errors in registration form validation and error handling.
* **Input Validation**: Fixed issues with input validation for the POST /items endpoint.
* **Image Handling**: Resolved bugs related to image upload handling on the backend.
* **Marker Logic**: Fixed rendering issues for dynamic markers on the map view.

---

## 3. Known Bugs and Defects
* **Profile Persistence**: While profiles can be edited, some specific fields may require additional schema updates in future iterations.
* **Map Query Optimization**: Distance calculations and geo-optimizations are currently functional but may require further optimization for extremely high item volumes.
