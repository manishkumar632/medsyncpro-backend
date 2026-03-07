# Product Overview

## Project Purpose
MedSyncPro is a comprehensive healthcare platform that connects patients, doctors, pharmacists, and administrators in a unified online medication and prescription management system. The platform streamlines healthcare delivery through digital consultations, appointment management, prescription handling, and professional verification workflows.

## Value Proposition
- **For Patients**: Easy access to verified healthcare professionals, online consultations, appointment booking, and prescription management
- **For Doctors**: Professional profile management, appointment scheduling, patient management, and document verification system
- **For Pharmacists**: Prescription fulfillment, inventory management, and sales tracking
- **For Administrators**: Platform oversight, user management, verification approvals, and analytics

## Key Features

### Authentication & Authorization
- Multi-role user registration (ADMIN, DOCTOR, PATIENT, PHARMACIST, AGENT)
- Email verification with token-based system
- JWT authentication with HttpOnly cookies for security
- Role-based access control (RBAC)
- Refresh token mechanism for session management
- Token blacklisting for secure logout

### Profile Management
- Single PATCH endpoint for partial profile updates
- Profile image and document uploads via Cloudinary CDN
- Transaction-safe updates with automatic rollback on failure
- Optimistic locking for concurrent update handling
- Self-update only security model

### Doctor Verification System
- Multi-step document submission workflow
- Support for medical licenses, certifications, and identity documents
- Admin/Agent review and approval process
- Status tracking (UNVERIFIED, PENDING, VERIFIED, REJECTED)
- Document type management with required/optional flags
- Cloudinary-based signed upload for security

### Appointment Management
- Patient-to-doctor appointment booking
- Multiple consultation types (IN_PERSON, VIDEO, CHAT)
- Appointment status workflow (PENDING, CONFIRMED, COMPLETED, CANCELLED)
- Auto-approval settings for doctors
- Real-time notifications via Firebase and SSE

### Notification System
- Multi-channel notifications (Email, Push, In-app)
- Event-driven architecture for appointment and verification events
- Firebase Cloud Messaging integration
- Server-Sent Events (SSE) for real-time updates
- Customizable notification preferences

### Search & Discovery
- Advanced doctor search with filters (speciality, location, availability)
- Public query endpoints for unauthenticated users
- Doctor profile visibility controls

### File Management
- Cloudinary cloud storage integration
- Support for images (JPEG, PNG) and documents (PDF)
- File size validation (10MB limit)
- Secure signed uploads
- CDN delivery for optimal performance

## Target Users

### Patients
- Individuals seeking medical consultations
- Users managing prescriptions and appointments
- People looking for verified healthcare professionals

### Doctors
- Medical professionals offering consultations
- Healthcare providers managing patient appointments
- Practitioners requiring professional verification

### Pharmacists
- Pharmacy staff fulfilling prescriptions
- Inventory managers tracking medication sales
- Healthcare providers in the medication supply chain

### Administrators & Agents
- Platform administrators managing users and content
- Verification agents reviewing professional credentials
- System operators monitoring platform health

## Use Cases

1. **Patient Journey**: Sign up → Find doctor → Book appointment → Attend consultation → Receive prescription
2. **Doctor Onboarding**: Register → Upload credentials → Await verification → Set up profile → Start accepting appointments
3. **Prescription Workflow**: Doctor creates prescription → Patient receives notification → Pharmacist fulfills order
4. **Verification Process**: Doctor submits documents → Agent reviews → Admin approves/rejects → Doctor notified
5. **Appointment Lifecycle**: Patient books → Doctor confirms → Consultation occurs → Follow-up scheduled
