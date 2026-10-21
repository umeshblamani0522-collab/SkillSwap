-- ============================================================
-- Development seed data.
-- All sample passwords are the SAME dev-only value: Password123!
-- stored below only as a BCrypt hash (never plain text).
-- See README.md "Development Login Credentials" for the full list.
-- ============================================================

INSERT INTO roles (name) VALUES ('USER'), ('ADMIN');

INSERT INTO skill_categories (name, description) VALUES
    ('Programming', 'General-purpose programming languages and paradigms'),
    ('Web Development', 'Frontend, backend, and full-stack web technologies'),
    ('Mobile Development', 'iOS, Android, and cross-platform mobile development'),
    ('Data Science', 'Statistics, data analysis, and data engineering'),
    ('AI/ML', 'Machine learning, deep learning, and artificial intelligence'),
    ('Cloud Computing', 'AWS, Azure, GCP, and cloud architecture'),
    ('Cybersecurity', 'Security fundamentals, ethical hacking, and defense'),
    ('UI/UX', 'User interface and user experience design'),
    ('Database', 'Relational and NoSQL database design and administration'),
    ('Languages', 'Spoken/written human languages'),
    ('Communication', 'Public speaking, writing, and interpersonal skills'),
    ('Marketing', 'Digital marketing, branding, and growth'),
    ('Finance', 'Personal finance, investing, and accounting'),
    ('Photography', 'Photography techniques and editing'),
    ('Music', 'Instruments, music theory, and production'),
    ('Career Skills', 'Resume writing, interviewing, and career growth'),
    ('Academic Skills', 'Study techniques and academic subjects'),
    ('Other', 'Anything that does not fit another category');

INSERT INTO skills (name, category_id, is_active) VALUES
    ('Java', (SELECT id FROM skill_categories WHERE name = 'Programming'), true),
    ('Python', (SELECT id FROM skill_categories WHERE name = 'Programming'), true),
    ('JavaScript', (SELECT id FROM skill_categories WHERE name = 'Programming'), true),
    ('Spring Boot', (SELECT id FROM skill_categories WHERE name = 'Web Development'), true),
    ('React', (SELECT id FROM skill_categories WHERE name = 'Web Development'), true),
    ('Node.js', (SELECT id FROM skill_categories WHERE name = 'Web Development'), true),
    ('SQL', (SELECT id FROM skill_categories WHERE name = 'Database'), true),
    ('PostgreSQL', (SELECT id FROM skill_categories WHERE name = 'Database'), true),
    ('AWS', (SELECT id FROM skill_categories WHERE name = 'Cloud Computing'), true),
    ('Docker', (SELECT id FROM skill_categories WHERE name = 'Cloud Computing'), true),
    ('UI/UX Design', (SELECT id FROM skill_categories WHERE name = 'UI/UX'), true),
    ('Figma', (SELECT id FROM skill_categories WHERE name = 'UI/UX'), true),
    ('Machine Learning', (SELECT id FROM skill_categories WHERE name = 'AI/ML'), true),
    ('Data Analysis', (SELECT id FROM skill_categories WHERE name = 'Data Science'), true),
    ('Public Speaking', (SELECT id FROM skill_categories WHERE name = 'Communication'), true),
    ('Spanish', (SELECT id FROM skill_categories WHERE name = 'Languages'), true),
    ('Guitar', (SELECT id FROM skill_categories WHERE name = 'Music'), true),
    ('Photography Basics', (SELECT id FROM skill_categories WHERE name = 'Photography'), true);

-- Dev users. Password for ALL sample accounts: Password123!
-- Hash below is a real BCrypt hash of that value (10 rounds).
INSERT INTO users (username, email, password, full_name, bio, location, languages, experience_years, is_suspended, is_enabled)
VALUES
    ('admin', 'admin@skillexchange.dev', '$2b$10$tcNkVt/qQc3CdI64Fn4awe.8q9I8CFRmdqvH2OJuEJVEwDfZ3Q.6S',
     'Platform Admin', 'Platform administrator account.', 'Remote', 'English', 10, false, true),
    ('alice_dev', 'alice@skillexchange.dev', '$2b$10$tcNkVt/qQc3CdI64Fn4awe.8q9I8CFRmdqvH2OJuEJVEwDfZ3Q.6S',
     'Alice Johnson', 'Backend engineer who loves teaching Java and Spring Boot. Looking to level up my design skills.',
     'Austin, TX', 'English, Spanish', 6, false, true),
    ('bob_design', 'bob@skillexchange.dev', '$2b$10$tcNkVt/qQc3CdI64Fn4awe.8q9I8CFRmdqvH2OJuEJVEwDfZ3Q.6S',
     'Bob Martinez', 'Product designer with 8 years of experience in UI/UX and Figma. Want to learn to code.',
     'Austin, TX', 'English', 8, false, true),
    ('carla_cloud', 'carla@skillexchange.dev', '$2b$10$tcNkVt/qQc3CdI64Fn4awe.8q9I8CFRmdqvH2OJuEJVEwDfZ3Q.6S',
     'Carla Nguyen', 'AWS-certified cloud architect. Happy to teach AWS/Docker in exchange for ML fundamentals.',
     'Seattle, WA', 'English, Vietnamese', 7, false, true),
    ('dev_dan', 'dan@skillexchange.dev', '$2b$10$tcNkVt/qQc3CdI64Fn4awe.8q9I8CFRmdqvH2OJuEJVEwDfZ3Q.6S',
     'Dan Okafor', 'Data scientist exploring machine learning. Want to pick up cloud deployment skills.',
     'Seattle, WA', 'English', 3, false, true);

INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'admin' AND r.name = 'ADMIN';
INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'admin' AND r.name = 'USER';
INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id FROM users u, roles r WHERE u.username IN ('alice_dev','bob_design','carla_cloud','dev_dan') AND r.name = 'USER';

-- Credit accounts (starter balance matches AuthService's welcome bonus for consistency)
INSERT INTO credit_accounts (user_id, balance, total_earned, total_spent)
    SELECT id, 10, 10, 0 FROM users;

-- Alice: teaches Java/Spring Boot/SQL, wants to learn UI/UX Design and AWS
INSERT INTO user_skills (user_id, skill_id, skill_type, skill_level, experience_description)
SELECT u.id, s.id, 'TEACH', 'EXPERT', 'Used daily in production backend systems for 6 years.'
FROM users u, skills s WHERE u.username = 'alice_dev' AND s.name IN ('Java', 'Spring Boot', 'SQL');

INSERT INTO user_skills (user_id, skill_id, skill_type, skill_level, experience_description)
SELECT u.id, s.id, 'LEARN', 'BEGINNER', 'Want to design better internal tools.'
FROM users u, skills s WHERE u.username = 'alice_dev' AND s.name IN ('UI/UX Design', 'AWS');

-- Bob: teaches UI/UX Design/Figma, wants to learn Java/Spring Boot
INSERT INTO user_skills (user_id, skill_id, skill_type, skill_level, experience_description)
SELECT u.id, s.id, 'TEACH', 'EXPERT', 'Shipped design systems for 3 SaaS products.'
FROM users u, skills s WHERE u.username = 'bob_design' AND s.name IN ('UI/UX Design', 'Figma');

INSERT INTO user_skills (user_id, skill_id, skill_type, skill_level, experience_description)
SELECT u.id, s.id, 'LEARN', 'BEGINNER', 'Want to prototype my own side projects end-to-end.'
FROM users u, skills s WHERE u.username = 'bob_design' AND s.name IN ('Java', 'Spring Boot');

-- Carla: teaches AWS/Docker, wants to learn Machine Learning
INSERT INTO user_skills (user_id, skill_id, skill_type, skill_level, experience_description)
SELECT u.id, s.id, 'TEACH', 'EXPERT', 'AWS Certified Solutions Architect, 7 years in cloud infra.'
FROM users u, skills s WHERE u.username = 'carla_cloud' AND s.name IN ('AWS', 'Docker');

INSERT INTO user_skills (user_id, skill_id, skill_type, skill_level, experience_description)
SELECT u.id, s.id, 'LEARN', 'BEGINNER', 'Curious about applying ML to infra anomaly detection.'
FROM users u, skills s WHERE u.username = 'carla_cloud' AND s.name IN ('Machine Learning');

-- Dan: teaches Machine Learning/Data Analysis, wants to learn AWS
INSERT INTO user_skills (user_id, skill_id, skill_type, skill_level, experience_description)
SELECT u.id, s.id, 'TEACH', 'ADVANCED', '3 years building ML pipelines for churn prediction.'
FROM users u, skills s WHERE u.username = 'dev_dan' AND s.name IN ('Machine Learning', 'Data Analysis');

INSERT INTO user_skills (user_id, skill_id, skill_type, skill_level, experience_description)
SELECT u.id, s.id, 'LEARN', 'BEGINNER', 'Want to deploy models to production myself.'
FROM users u, skills s WHERE u.username = 'dev_dan' AND s.name IN ('AWS');

-- Availability: everyone free weekday evenings in their own timezone (illustrative sample data)
INSERT INTO availability (user_id, day_of_week, start_time, end_time, timezone)
SELECT id, 'MONDAY', '18:00', '21:00', 'America/Chicago' FROM users WHERE username = 'alice_dev';
INSERT INTO availability (user_id, day_of_week, start_time, end_time, timezone)
SELECT id, 'WEDNESDAY', '18:00', '21:00', 'America/Chicago' FROM users WHERE username = 'alice_dev';
INSERT INTO availability (user_id, day_of_week, start_time, end_time, timezone)
SELECT id, 'MONDAY', '19:00', '22:00', 'America/Chicago' FROM users WHERE username = 'bob_design';
INSERT INTO availability (user_id, day_of_week, start_time, end_time, timezone)
SELECT id, 'TUESDAY', '17:00', '20:00', 'America/Los_Angeles' FROM users WHERE username = 'carla_cloud';
INSERT INTO availability (user_id, day_of_week, start_time, end_time, timezone)
SELECT id, 'TUESDAY', '17:30', '20:30', 'America/Los_Angeles' FROM users WHERE username = 'dev_dan';

-- A sample pending exchange request so the "Find Compatible User -> Match % -> Request" flow has data to show immediately
INSERT INTO exchange_requests (sender_id, receiver_id, offered_skill_id, requested_skill_id, duration_hours, message, status, match_percentage, expires_at)
SELECT
    (SELECT id FROM users WHERE username = 'alice_dev'),
    (SELECT id FROM users WHERE username = 'bob_design'),
    (SELECT id FROM skills WHERE name = 'Java'),
    (SELECT id FROM skills WHERE name = 'UI/UX Design'),
    2.0,
    'I would love to trade Java knowledge for some UI/UX mentorship on my side project!',
    'PENDING',
    82.5,
    now() + interval '7 days';
