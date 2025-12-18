# University ERP Test Plan
---

## 1. Introduction
This Test Plan defines the strategy, scope, test cases, and expected results for verifying the functionality, reliability, and security of the University ERP desktop application. The application includes Student, Instructor, and Admin workflows, and must satisfy role-based access rules and “Maintenance Mode” restrictions.

---

## 2. Seeded Accounts

- `a1:a1`  
- `i1:i1`  
- `s1:s1`  
- `s2:s2`

---

## 3. Test Data (Seed DB)
Your seed data includes:

### AuthDB
- 1 Admin  
- 1 Instructor  
- 2 Students  

### ERPDB
- Courses: CSE121, CSE201 
- Instructors assigned appropriately  
- Student “s1” enrolled in at least one section  
- Student “s2” not enrolled  

---

## 4. Test Scope
Testing covers:

- Login and authentication  
- Student workflows  
- Instructor workflows  
- Admin workflows  
- Role-based restrictions  
- Maintenance mode behavior  
- Security (password hashing, DB isolation)  
- Data integrity (capacity checks, duplicates)  
- UI/UX elements  
- Performance sanity tests  

---

## 5. Acceptance Test Cases

### **5.1 Login Tests**
| ID | Test Case | Steps | Expected Result |
|----|-----------|--------|-----------------|
| L1 | Wrong password rejected | Enter invalid password for `s1` | Error: “Incorrect username or password” |
| L2 | Correct login opens correct dashboard | Login as `i1` | Instructor dashboard loads |
| L3 | Role-based dashboard | Login as each role | Student/Instructor/Admin UI appears correctly |
| L4 | Password hash only | Check AuthDB | Only hashed passwords exist |

---

### **5.2 Student Tests**
| ID | Test Case | Steps | Expected Result |
|----|-----------|--------|-----------------|
| S1 | View course catalog | Student → Catalog | Table loads (code/title/credits/capacity/instructor) |
| S2 | Register for available section | Register for CSE121-2 with free seats | Success + added to My Registrations |
| S3 | Duplicate registration | Try registering again | Error: “Already registered” |
| S4 | Register in full section | Fill capacity manually -> try register | Error: “Section full” |
| S5 | Drop before deadline | Drop section | Enrollment removed |
| S6 | View grades | Student -> Grades | All components + final grade displayed |
| S7 | Transcript export | Click Export CSV/PDF | File downloads/open successfully |

---

### **5.3 Instructor Tests**
| ID | Test Case | Steps | Expected Result |
|----|-----------|--------|-----------------|
| I1 | View only own sections | Login as `i1` | Only assigned sections visible |
| I2 | Enter assessment scores | Select section -> Enter quiz/midterm/final | Data saved |
| I3 | Compute final grade | Click Compute Final | Final grade appears using weighting rule |
| I4 | Edit not-owned section | Try editing another instructor’s section | Error: “Not your section” |
| I5 | Export grade CSV (optional) | Click Export CSV | File downloads |

---

### **5.4 Admin Tests**
| ID | Test Case | Steps | Expected Result |
|----|-----------|--------|-----------------|
| A1 | Create new user | Add username + role | Appears in AuthDB |
| A2 | Create course | Add course in UI | Course listed |
| A3 | Create section | Add section with capacity/time | Section listed |
| A4 | Assign instructor | Select instructor for section | Instructor assigned |
| A5 | Maintenance ON | Toggle ON | Banner visible; students/instructors cannot modify data |
| A6 | Maintenance OFF | Toggle OFF | Normal behavior restored |

---

### **5.5 Maintenance Mode Tests**
| ID | Description | Steps | Expected Result |
|----|-------------|--------|-----------------|
| M1 | Student attempts register | Maintenance ON -> try register | Blocked: “Maintenance is ON” |
| M2 | Instructor attempts grade update | Maintenance ON -> enter marks | Blocked with message |
| M3 | UI shows banner | Login while ON | Visible red/yellow banner |
| M4 | Maintenance OFF restores writes | Turn OFF -> try actions | Writes now work |

---

### **5.6 Security Tests**
| ID | Test | Steps | Expected |
|----|------|--------|----------|
| SE1 | Real passwords not stored | Inspect AuthDB | Only bcrypt hashes |
| SE2 | ERPDB contains no passwords | Inspect ERPDB | No password column |
| SE3 | Access rule checks | Try unauthorized actions | Blocked with friendly message |

---

### **5.7 Edge & Negative Tests**
| ID | Test Case | Steps | Expected Result |
|----|------------|--------|-----------------|
| N1 | Negative capacity | Create section with -10 | Error: “Invalid capacity” |
| N2 | Register after deadline | Set past deadline register | Disabled |
| N3 | Student view another record | Try editing other student’s enrollment | Not possible |
| N4 | Instructor grading wrong section | Try grading foreign section | Not possible |
| N5 | Duplicate enrollment prevention | Try inserting manually | Prevented at service layer |

---

### **5.8 Performance Tests**
| ID | Test | Steps | Expected |
|----|------|--------|---------|
| P1 | Catalog load speed | Load catalog (100+ rows) | < 2–3 seconds |
| P2 | App startup | Run JAR | Starts without freezing |

---

## 6. Pass/Fail Criteria
- A test **passes** if actual behavior matches expected result.  
- A test **fails** if behavior deviates or error messages do not appear correctly.  

Critical failures (e.g., login, registration, grade computation) must be fixed.

---

## 7. Risks & Assumptions
**Risks:**  
- DB connection errors  
- Incorrect test data  
- OS display variations  

**Assumptions:**  
- Testers use provided seed DB  
- Testers use correct JDK version  

