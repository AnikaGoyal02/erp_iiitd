USE authdb;

INSERT INTO users_auth (username, role, password_hash)
VALUES
('a1','ADMIN','$2a$10$RLOHNKbaBcg7Oliawvy8g.uELcelmR2W8yxukKUPmVyD3rEqPVwHq'),
('i1','INSTRUCTOR','$2a$10$L3H6uD0BTN.CJo21nAwiQ.K.5qvrDRqtevRm6c5Ts.d2y5zg9fCEG'),
('s1','STUDENT','$2a$10$yhltMtlrylsNLVNLvMpn9eCbFO7NJprSeQnqpabYmmg7Wx7E/syFG'),
('s2','STUDENT','$2a$10$o7WqR9XpslAgLUj.GdQ7qOcQQAZqVIo.UmLrN6Wp97Ve6nnD2KMfa');

-- passwords = username

USE erpdb;

INSERT INTO instructors (user_id, department, email)
VALUES (
  (SELECT user_id FROM authdb.users_auth WHERE username='i1'),
  'Computer Science',
  'i1@iiitd.ac.in'
);

INSERT INTO students (user_id, roll_no, program, year, email)
VALUES
((SELECT user_id FROM authdb.users_auth WHERE username='s1'),
 '2024175', 'B.Tech CSE', 2, 's1@iiitd.ac.in'),
((SELECT user_id FROM authdb.users_auth WHERE username='s2'),
 '2024072', 'B.Tech CSAI', 2, 's2@iiitd.ac.in');

INSERT INTO courses (code, title, credits, description)
VALUES
('CSE121','Discrete Mathematics',3,'Fundamentals of DM'),
('CSE201','Advanced Programming',3,'Programming in Java with OOPs');

INSERT INTO sections (course_id, instructor_id, day_time, room, capacity, semester, year)
VALUES
((SELECT course_id FROM courses WHERE code='CSE121'),
 (SELECT instructor_id FROM instructors LIMIT 1),
 'Mon/Wed 9:00-10:00',
 'B003', 150, 'Monsoon', 2),

((SELECT course_id FROM courses WHERE code='CSE201'),
 (SELECT instructor_id FROM instructors LIMIT 1),
 'Tue/Thu 15:00-16:30',
 'C102', 600, 'Monsoon', 2);

INSERT INTO enrollments (student_id, section_id)
VALUES
((SELECT student_id FROM students WHERE roll_no='2024175'),
 (SELECT section_id FROM sections WHERE course_id=(SELECT course_id FROM courses WHERE code='CSE121'))),

((SELECT student_id FROM students WHERE roll_no='2024072'),
 (SELECT section_id FROM sections WHERE course_id=(SELECT course_id FROM courses WHERE code='CSE201'))
);

INSERT INTO settings (`key`, `value`)
VALUES ('maintenance_mode','false')
ON DUPLICATE KEY UPDATE `value`='false';
