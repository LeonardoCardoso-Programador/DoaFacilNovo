

# Project Video 

https://github.com/user-attachments/assets/c7e1790f-0db4-4fc1-ada2-360b0d5850d0

# DoaFacil
Doa Fácil is a mobile application developed as part of the Professional Aptitude Test (PAP) at the Ruiz Costa Professional School (ERC).
The main objective is to connect food donors with people in vulnerable situations, facilitating quick and direct donations based on users' geographic locations.
The platform promotes solidarity, reduces food waste, and encourages community participation.

The application acts as an intermediary between donors and beneficiaries, allowing the creation, viewing, and requesting of donations in an intuitive and secure manner.
Developed for Android, it uses modern technologies to ensure scalability and accessibility.

# Main Features

Account Creation and Login: User registration with email verification and secure authentication.

Profile Editing: Updating personal information, profile picture, and description.

Add Donations: Creating new donations with images, title, description, pickup schedules, listing time, and location (integrated with Google Maps).

View Donations: List of available donations, with specific details and a map for location.

Send Requests: Form to request donations, including desired pickup time and personalized message.

Donation Management: Viewing, editing, and deleting donations created by the user.

View Requests: Received (accept/reject) and sent (filter by status: in progress, accepted, rejected).

Confirmed Donations: Tracking accepted donations, with confirmation codes for pickup validation.

Side Menu: Quick navigation to home, my donations, locations, profile, view other users, requests, and logout.

View Other Users: Member profiles, including donations made and statistics.

Map Integration: Display of locations via Google Maps SDK.

#Technologies Used

Development Environment: Android Studio (IDE based on IntelliJ IDEA).

Programming Language: Kotlin (for back-end logic, authentication, and integration).

Graphical Interface: XML (for layouts and visual elements).

Design and Prototyping: Figma (for creating wireframes and navigation simulation).

Maps and Location: Google Maps SDK for Android API.

Backend and Storage: Firebase (Authentication for login/registration, Firestore for real-time database, Storage for images and files).

Others: Integration with emulators for testing on different devices.

#Application Structure

Database (Firestore): Stores user data, donations, and requests.

Navigation Flow: Side menu for quick access to main features.

Security: Authentication via Firebase; permissions for location and storage.

#Installation and Configuration
Requirements:
Android Studio installed.
Firebase account (for Authentication, Firestore, and Storage).
Google Maps API key (configured in Google Cloud Console).
Steps:
Clone the repository: git clone https://github.com/your-username/doa-facil.git.
Open the project in Android Studio.
Configure Firebase: Add the google-services.json file to the app/ directory.
Add the Google Maps API key to the AndroidManifest.xml file.
Sync Gradle and build the project.
Run on an emulator or Android device.

# Installation and Configuration
Requirements

Android Studio installed.

Firebase account (for Authentication, Firestore, and Storage).

Google Maps API key (configured in Google Cloud Console).

Steps

Clone the repository:

git clone https://github.com/your-username/doa-facil.git


Open the project in Android Studio.

Configure Firebase: Add the google-services.json file to the app/ directory.

Add the Google Maps API key to the AndroidManifest.xml file.

Sync Gradle and build the project.

Run on an emulator or Android device.


# Usage

Donor: Create donations, manage received requests, and confirm pickups.

Beneficiary: View available donations, send requests, and track status.

Tests: Use emulators to simulate different location and authentication scenarios.

# Limitations and Future Improvements
Limitations: Absence of real-time notification system; initial focus on basic features.
Suggestions: Add push notifications (via Firebase Cloud Messaging), multi-language support, social media integration, and social impact reports.

# Contributions
Contributions are welcome! To contribute:
Fork the repository.
Create a branch for your feature (git checkout -b feature/new-feature).
Commit your changes (git commit -m 'Add new feature').
Push to the branch (git push origin feature/new-feature).
Open a Pull Request.

#License
This project is licensed under the MIT License - see the LICENSE file for details.

