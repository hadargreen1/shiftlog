Based on the files you've provided, I'll create a README.md file for your project. The README will include an overview of the app, key features, and explanations of important components.

---

# ShiftLog

ShiftLog is a mobile application designed to help users manage their work shifts, calculate earnings, and keep track of their working hours. The app integrates with Firebase to securely store user data, including shift details and user information. It also provides features to export data in PDF and Excel formats.

## Key Features

- **User Authentication**: Secure user registration and login using Firebase Authentication.
- **Shift Management**: Users can start and stop shifts, enter shift times manually, and submit shift data to the Firebase Realtime Database.
- **Earnings Calculation**: Automatically calculates total earnings based on hourly wage and shift duration.
- **Export Functionality**: Export shift data to PDF and Excel formats for easy sharing and record-keeping.
- **Personal Information Management**: Users can view and update their personal information, including hourly wage, through the Account Info section.
- **Custom UI Components**: Utilizes Material Design components for a consistent and modern user interface.

## Project Structure

- **MainActivity.kt**: The main dashboard of the app where users can navigate to different sections.
- **RegisterActivity.kt**: Handles user registration, including saving user data to Firebase Firestore and Realtime Database.
- **LoginActivity.kt**: Manages user login and authentication.
- **SubmitShiftActivity.kt**: Allows users to start, stop, and submit shift times. Shift data is saved in the Firebase Realtime Database.
- **PayManagementActivity.kt**: Users can calculate net income based on monthly salary, deductions, and bonuses. Provides options to export data as PDF or Excel.
- **AccountInfoActivity.kt**: Displays and updates user's personal information, including their hourly wage, which is fetched from Firebase Realtime Database.
- **BaseActivity.kt**: A base class for activities that sets up the toolbar and navigation drawer.
- **CalendarUtility.kt**: Utility class for handling calendar-related operations, such as date selection.

## Special Features

- **Firebase Integration**: The app uses Firebase for authentication, Firestore for storing user profiles, and Realtime Database for storing shift data.
- **PDF and Excel Export**: Users can export their shift data and earnings reports in PDF and Excel formats, using the iText and Apache POI libraries, respectively.
- **Material CalendarView**: The app incorporates a custom calendar view for easy date selection, which is integrated into the SubmitShiftActivity.
- **Responsive Design**: The app is designed with a responsive layout, ensuring a seamless user experience across different screen sizes.

## Getting Started

To get started with the project:

1. **Clone the repository**: Download the project files to your local machine.
2. **Set up Firebase**: Create a Firebase project and add your `google-services.json` file to the app's `app/` directory.
3. **Build the project**: Open the project in Android Studio, sync the Gradle files, and build the project.
4. **Run the app**: Deploy the app on an Android device or emulator.

## Dependencies

- **Firebase Authentication**: `com.google.firebase:firebase-auth-ktx`
- **Firebase Firestore**: `com.google.firebase:firebase-firestore-ktx`
- **Firebase Realtime Database**: `com.google.firebase:firebase-database-ktx`
- **Material CalendarView**: `com.github.prolificinteractive:material-calendarview:2.0.1`
- **iText PDF**: `com.itextpdf:itext7-core:7.2.5`
- **Apache POI for Excel**: `org.apache.poi:poi-ooxml:5.2.2`

## How to Contribute

If you would like to contribute to the project:

1. Fork the repository.
2. Create a new branch with your feature or bugfix.
3. Commit and push your changes to the branch.
4. Submit a pull request for review.
