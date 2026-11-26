-- =====================================================================
--  Sample data for demo / viva
--  Run after schema.sql:  mysql -u root -p courtroom_db < database/seed.sql
--
--  Login accounts (username / password):
--    Registrar : registrar / admin123
--    Judges    : judge1, judge2, judge3 / judge123
--    Lawyers   : lawyer1, lawyer2, lawyer3, lawyer4 / lawyer123
-- =====================================================================
USE courtroom_db;

INSERT INTO courtrooms (id, room_no, name) VALUES
    (1, '1', 'Civil Court'),
    (2, '2', 'Criminal Court'),
    (3, '3', 'Family Court'),
    (4, '4', 'Consumer & Property Court');

-- Password hashes generated with com.court.util.PasswordUtil
INSERT INTO users (id, name, username, password_hash, role, email, phone) VALUES
    (1, 'Rajesh Khanna',        'registrar', 'ahuwAyODN5xazkpa2dIGLQ==$l5KUA+nMRMJdiO+rCr4zPhGNAUuf943DgqJq+INfQdI=', 'REGISTRAR', 'registrar@court.gov.in', '9876500001'),
    (2, 'Justice Anil Mehta',   'judge1',    'pLlmlF5feQDEZJzgaDE73g==$MrTlh5ymWEwDWnNHMAqFa71/HX620njq3hJIgmaozKU=', 'JUDGE',     'anil.mehta@court.gov.in', '9876500002'),
    (3, 'Justice Priya Sharma', 'judge2',    'pLlmlF5feQDEZJzgaDE73g==$MrTlh5ymWEwDWnNHMAqFa71/HX620njq3hJIgmaozKU=', 'JUDGE',     'priya.sharma@court.gov.in', '9876500003'),
    (4, 'Justice R. S. Gill',   'judge3',    'pLlmlF5feQDEZJzgaDE73g==$MrTlh5ymWEwDWnNHMAqFa71/HX620njq3hJIgmaozKU=', 'JUDGE',     'rs.gill@court.gov.in', '9876500004'),
    (5, 'Adv. Neha Verma',      'lawyer1',   'VK/8kbCwmS6duIlLkV/vsQ==$C0Vj0QaKPgAbWQJmp5NDJo2zjX05NmPMUmVpKCKdYi4=', 'LAWYER',    'neha.verma@gmail.com', '9812300001'),
    (6, 'Adv. Arjun Singh',     'lawyer2',   'VK/8kbCwmS6duIlLkV/vsQ==$C0Vj0QaKPgAbWQJmp5NDJo2zjX05NmPMUmVpKCKdYi4=', 'LAWYER',    'arjun.singh@gmail.com', '9812300002'),
    (7, 'Adv. Kavita Bansal',   'lawyer3',   'VK/8kbCwmS6duIlLkV/vsQ==$C0Vj0QaKPgAbWQJmp5NDJo2zjX05NmPMUmVpKCKdYi4=', 'LAWYER',    'kavita.bansal@gmail.com', '9812300003'),
    (8, 'Adv. Mohit Arora',     'lawyer4',   'VK/8kbCwmS6duIlLkV/vsQ==$C0Vj0QaKPgAbWQJmp5NDJo2zjX05NmPMUmVpKCKdYi4=', 'LAWYER',    'mohit.arora@gmail.com', '9812300004');

INSERT INTO judges (user_id, designation, courtroom_id) VALUES
    (2, 'Civil Judge (Senior Division)', 1),
    (3, 'Additional Sessions Judge',     2),
    (4, 'Principal Judge, Family Court', 3);

INSERT INTO lawyers (user_id, bar_id) VALUES
    (5, 'PB/1021/2014'),
    (6, 'PB/2210/2016'),
    (7, 'PB/3145/2018'),
    (8, 'PB/4402/2020');

-- Court holidays (hearings cannot be scheduled on these days)
INSERT INTO holidays (holiday_date, description) VALUES
    ('2026-12-25', 'Christmas'),
    ('2027-01-26', 'Republic Day'),
    ('2027-03-04', 'Holi'),
    ('2027-04-14', 'Ambedkar Jayanti'),
    ('2027-08-15', 'Independence Day'),
    ('2027-10-02', 'Gandhi Jayanti');
