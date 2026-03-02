# MedSyncPro API Documentation

This document provides a comprehensive list of all backend API endpoints available in the MedSyncPro application, along with `curl` examples.

> **Note on Authentication:** Endpoints requiring authentication should include the authentication cookie (`Cookie: access_token=<token>`). In the `curl` commands below, a placeholder is used for tokens and credentials.

---

## 1. Authentication (`/api/auth`)

### Get Current User
```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Cookie: access_token=<your_access_token>"
```

### Register New User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "Password123!",
    "role": "PATIENT"
  }'
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "Password123!"
  }'
```

### Refresh Token
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Cookie: refresh_token=<your_refresh_token>"
```

### Logout (Current Device)
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Cookie: access_token=<your_access_token>; refresh_token=<your_refresh_token>"
```

### Logout All Devices
```bash
curl -X POST http://localhost:8080/api/auth/logout-all \
  -H "Cookie: access_token=<your_access_token>"
```

### Verify Email
```bash
curl -X POST http://localhost:8080/api/auth/verify-email \
  -H "Content-Type: application/json" \
  -d '{"token": "your_verification_token"}'
```

### Resend Verification Email
```bash
curl -X POST "http://localhost:8080/api/auth/resend-verification?email=john@example.com"
```

---

## 2. Profile & User Management (`/api/users`)

### Get Lightweight Session Validation
```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Cookie: access_token=<your_access_token>"
```

### Get Full Profile Details
```bash
curl -X GET http://localhost:8080/api/users/profile \
  -H "Cookie: access_token=<your_access_token>"
```

### Update Profile (Simple JSON Update)
```bash
curl -X PUT http://localhost:8080/api/users/profile \
  -H "Content-Type: application/json" \
  -H "Cookie: access_token=<your_access_token>" \
  -d '{
    "phone": "+1234567890",
    "address": "123 Main St"
  }'
```

### Update Profile (Multipart - includes files)
```bash
curl -X PATCH http://localhost:8080/api/users/profile \
  -H "Cookie: access_token=<your_access_token>" \
  -F "profileImage=@/path/to/image.jpg" \
  -F "profile={\"phone\": \"+1234567890\"};type=application/json"
```

### Get Verification Status
```bash
curl -X GET http://localhost:8080/api/users/me/verification-status \
  -H "Cookie: access_token=<your_access_token>"
```

### Get Required Documents Checklist
```bash
curl -X GET http://localhost:8080/api/users/me/required-documents \
  -H "Cookie: access_token=<your_access_token>"
```

### Upload Single Verification Document
```bash
curl -X POST http://localhost:8080/api/users/me/documents/MEDICAL_LICENSE \
  -H "Cookie: access_token=<your_access_token>" \
  -F "file=@/path/to/document.pdf"
```

### Delete Single Verification Document
```bash
curl -X DELETE http://localhost:8080/api/users/me/documents/MEDICAL_LICENSE \
  -H "Cookie: access_token=<your_access_token>"
```

### Submit Verification For Admin Review
```bash
curl -X POST http://localhost:8080/api/users/me/submit-verification \
  -H "Cookie: access_token=<your_access_token>"
```

---

## 3. Notifications (`/api/notifications`)

### Connect to SSE Stream (Realtime events)
```bash
curl -N -X GET http://localhost:8080/api/notifications/stream \
  -H "Cookie: access_token=<your_access_token>"
```

### Get Notifications List
```bash
curl -X GET http://localhost:8080/api/notifications \
  -H "Cookie: access_token=<your_access_token>"
```

### Get Unread Notification Count
```bash
curl -X GET http://localhost:8080/api/notifications/unread-count \
  -H "Cookie: access_token=<your_access_token>"
```

### Mark Notification as Read
```bash
curl -X PUT http://localhost:8080/api/notifications/123e4567-e89b-12d3-a456-426614174000/read \
  -H "Cookie: access_token=<your_access_token>"
```

---

## 4. Doctor Settings (`/api/doctor/settings`)

### Get All Doctor Settings
```bash
curl -X GET http://localhost:8080/api/doctor/settings \
  -H "Cookie: access_token=<your_access_token>"
```

### Professional Info
```bash
# Get
curl -X GET http://localhost:8080/api/doctor/settings/professional \
  -H "Cookie: access_token=<your_access_token>"

# Update
curl -X PUT http://localhost:8080/api/doctor/settings/professional \
  -H "Content-Type: application/json" \
  -H "Cookie: access_token=<your_access_token>" \
  -d '{ "specialty": "Cardiology", "experienceYears": 10 }'
```

### Clinics Management
```bash
# Get Clinics
curl -X GET http://localhost:8080/api/doctor/settings/clinics \
  -H "Cookie: access_token=<your_access_token>"

# Add Clinic
curl -X POST http://localhost:8080/api/doctor/settings/clinics \
  -H "Content-Type: application/json" \
  -H "Cookie: access_token=<your_access_token>" \
  -d '{ "name": "Heart Clinic", "address": "123 Health Ave" }'

# Update Clinic
curl -X PUT http://localhost:8080/api/doctor/settings/clinics/{id} \
  -H "Content-Type: application/json" \
  -H "Cookie: access_token=<your_access_token>" \
  -d '{ "name": "Updated Heart Clinic", "address": "123 Health Ave" }'

# Delete Clinic
curl -X DELETE http://localhost:8080/api/doctor/settings/clinics/{id} \
  -H "Cookie: access_token=<your_access_token>"
```

### Availability
```bash
# Get
curl -X GET http://localhost:8080/api/doctor/settings/availability \
  -H "Cookie: access_token=<your_access_token>"

# Update
curl -X PUT http://localhost:8080/api/doctor/settings/availability \
  -H "Content-Type: application/json" \
  -H "Cookie: access_token=<your_access_token>" \
  -d '{ "monday": {"enabled": true, "slots": [...]} }'
```

### Consultation Settings
```bash
# Get
curl -X GET http://localhost:8080/api/doctor/settings/consultation \
  -H "Cookie: access_token=<your_access_token>"

# Update
curl -X PUT http://localhost:8080/api/doctor/settings/consultation \
  -H "Content-Type: application/json" \
  -H "Cookie: access_token=<your_access_token>" \
  -d '{ "fee": 150, "durationMinutes": 30 }'
```

### Security & Privacy Settings
```bash
# Get Privacy
curl -X GET http://localhost:8080/api/doctor/settings/privacy \
  -H "Cookie: access_token=<your_access_token>"

# Update Privacy
curl -X PUT http://localhost:8080/api/doctor/settings/privacy \
  -H "Content-Type: application/json" \
  -H "Cookie: access_token=<your_access_token>" \
  -d '{ "showPhone": false, "showEmail": false }'

# Toggle Two-Factor Auth
curl -X PUT http://localhost:8080/api/doctor/settings/security/two-factor \
  -H "Content-Type: application/json" \
  -H "Cookie: access_token=<your_access_token>" \
  -d '{ "enabled": true }'

# Change Password
curl -X POST http://localhost:8080/api/doctor/settings/security/change-password \
  -H "Content-Type: application/json" \
  -H "Cookie: access_token=<your_access_token>" \
  -d '{ "currentPassword": "OldPassword123!", "newPassword": "NewPassword123!" }'
```

### Account Actions
```bash
# Get Account Summary
curl -X GET http://localhost:8080/api/doctor/settings/account/summary \
  -H "Cookie: access_token=<your_access_token>"

# Deactivate Account
curl -X POST http://localhost:8080/api/doctor/settings/account/deactivate \
  -H "Cookie: access_token=<your_access_token>"

# Delete Account
curl -X POST http://localhost:8080/api/doctor/settings/account/delete \
  -H "Cookie: access_token=<your_access_token>"
```

---

## 5. Doctor Search (`/api/doctors`)

### Search Doctors
```bash
# Basic search query (public)
curl -X GET "http://localhost:8080/api/doctors/search?q=cardio&page=0&size=10&sort=name&direction=asc"
```

### Get Doctor Profile (Public)
```bash
curl -X GET http://localhost:8080/api/doctors/a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

---

## 6. Admin Endpoints (`/api/admin`)
*(Requires ADMIN Role Authentication)*

### Get Dashboard Stats
```bash
curl -X GET http://localhost:8080/api/admin/stats \
  -H "Cookie: access_token=<admin_access_token>"
```

### List Users with Filter
```bash
curl -X GET "http://localhost:8080/api/admin/users?role=DOCTOR&approved=false&search=priya&page=0&size=20" \
  -H "Cookie: access_token=<admin_access_token>"
```

### List Pending Approvals
```bash
curl -X GET http://localhost:8080/api/admin/users/pending \
  -H "Cookie: access_token=<admin_access_token>"
```

### Admin User Actions
```bash
# Approve User
curl -X PATCH http://localhost:8080/api/admin/users/{id}/approve \
  -H "Cookie: access_token=<admin_access_token>"

# Reject User
curl -X PATCH http://localhost:8080/api/admin/users/{id}/reject \
  -H "Cookie: access_token=<admin_access_token>"

# Suspend User
curl -X PATCH http://localhost:8080/api/admin/users/{id}/suspend \
  -H "Cookie: access_token=<admin_access_token>"

# Activate User
curl -X PATCH http://localhost:8080/api/admin/users/{id}/activate \
  -H "Cookie: access_token=<admin_access_token>"
```

### Admin Verifications
```bash
# Get All Verifications
curl -X GET http://localhost:8080/api/admin/verifications \
  -H "Cookie: access_token=<admin_access_token>"

# Get Specific Verification Detail
curl -X GET http://localhost:8080/api/admin/verifications/{id} \
  -H "Cookie: access_token=<admin_access_token>"

# Approve Verification Request
curl -X POST http://localhost:8080/api/admin/verifications/{id}/approve \
  -H "Cookie: access_token=<admin_access_token>"

# Reject Verification Request
curl -X POST http://localhost:8080/api/admin/verifications/{id}/reject \
  -H "Content-Type: application/json" \
  -H "Cookie: access_token=<admin_access_token>" \
  -d '{ "comment": "Documents are not clear." }'
```

### Admin FCM & Notifications
```bash
# Register Admin FCM Token
curl -X POST http://localhost:8080/api/admin/fcm-token \
  -H "Content-Type: application/json" \
  -H "Cookie: access_token=<admin_access_token>" \
  -d '{ "token": "your_fcm_token" }'

# Get Admin Notifications
curl -X GET http://localhost:8080/api/admin/notifications \
  -H "Cookie: access_token=<admin_access_token>"

# Mark Admin Notification as Read
curl -X PUT http://localhost:8080/api/admin/notifications/{id}/read \
  -H "Cookie: access_token=<admin_access_token>"
```

---

## 7. Files Redirect (`/files`)

### Public File Access
```bash
curl -i -X GET http://localhost:8080/files/image.jpg
# (Returns 301 Moved Permanently to Cloudinary)
```
