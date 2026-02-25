# Admin Statistics API — Walkthrough

## Changes Made

### Backend (5 files)

| File | Action | Purpose |
|------|--------|---------|
| [UserRepository.java](file:///c:/Users/manis/Documents/infosys/medsyncpro-backend/src/main/java/com/medsyncpro/repository/UserRepository.java) | Modified | Added [countByRoleAndDeletedFalse(Role)](file:///c:/Users/manis/Documents/infosys/medsyncpro-backend/src/main/java/com/medsyncpro/repository/UserRepository.java#20-21) + [countPendingApprovals()](file:///c:/Users/manis/Documents/infosys/medsyncpro-backend/src/main/java/com/medsyncpro/repository/UserRepository.java#22-24) |
| [AdminStatsResponse.java](file:///c:/Users/manis/Documents/infosys/medsyncpro-backend/src/main/java/com/medsyncpro/dto/AdminStatsResponse.java) | **New** | DTO with `totalPatients`, `totalDoctors`, `totalPharmacists`, `totalUsers`, `pendingApprovals` |
| [AdminService.java](file:///c:/Users/manis/Documents/infosys/medsyncpro-backend/src/main/java/com/medsyncpro/service/AdminService.java) | **New** | Aggregates role counts from repository |
| [AdminController.java](file:///c:/Users/manis/Documents/infosys/medsyncpro-backend/src/main/java/com/medsyncpro/controller/AdminController.java) | **New** | `GET /api/admin/stats` — secured with `@PreAuthorize("hasRole('ADMIN')")` |
| [SecurityConfig.java](file:///c:/Users/manis/Documents/infosys/medsyncpro-backend/src/main/java/com/medsyncpro/config/SecurityConfig.java) | Modified | Added `@EnableMethodSecurity` |

### Frontend (1 file)

| File | Action | Purpose |
|------|--------|---------|
| [KpiCards.jsx](file:///c:/Users/manis/Documents/infosys/online-medication/frontend/medsyncpro/app/admin/dashboard/components/KpiCards.jsx) | Modified | Fetches live data from `/api/admin/stats` with loading/error states |

---

## Edge Cases Covered

| Scenario | Response |
|----------|----------|
| No auth token | **401** — JWT filter blocks |
| Valid token, non-admin role | **403** — `@PreAuthorize` blocks |
| Expired token | **401** — "Token has expired" |
| Blacklisted token | **401** — "Token has been revoked" |
| Token version mismatch (role changed) | **401** — "Token version mismatch" |
| Deleted admin user | **401** — "User not found or deleted" |
| Soft-deleted users | Excluded via `deleted = false` filter |

---

## Build Verification

```
mvn compile -q → Exit code: 0 ✅
```

---

## curl Commands for Postman Testing

### 1. Login as Admin
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@medsyncpro.com","password":"your_password"}' \
  -c cookies.txt
```

### 2. Get Admin Stats ✅
```bash
curl -X GET http://localhost:8080/api/admin/stats \
  -b cookies.txt
```

**Expected response:**
```json
{
  "success": true,
  "message": "Admin statistics retrieved successfully",
  "data": {
    "totalPatients": 5,
    "totalDoctors": 3,
    "totalPharmacists": 2,
    "totalUsers": 10,
    "pendingApprovals": 1
  },
  "errors": null
}
```

### 3. Test as Non-Admin (expect 403)
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"patient@medsyncpro.com","password":"your_password"}' \
  -c patient_cookies.txt

curl -X GET http://localhost:8080/api/admin/stats \
  -b patient_cookies.txt
```

**Expected:** `403 Forbidden`

### 4. Test Without Auth (expect 401)
```bash
curl -X GET http://localhost:8080/api/admin/stats
```

**Expected:** `401 Unauthorized`

> [!TIP]
> In Postman, use **Cookie Manager** instead of `-c`/`-b` flags. Login first, then Postman auto-sends the `access_token` cookie on subsequent requests to the same domain.
