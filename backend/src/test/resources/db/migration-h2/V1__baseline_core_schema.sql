CREATE TABLE IF NOT EXISTS users (
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    refresh_token_expires_at DATETIME(6) NULL,
    updated_at DATETIME(6) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    refresh_token VARCHAR(1000) NULL,
    password VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'USER') NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS resumes (
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    updated_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    file_url VARCHAR(500) NULL,
    content VARCHAR(5000) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_resumes_user_id (user_id),
    CONSTRAINT fk_resumes_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cover_letters (
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    updated_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(5000) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_cover_letters_user_id (user_id),
    CONSTRAINT fk_cover_letters_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS application_documents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    cover_letter_text VARCHAR(5000) NULL,
    file_url VARCHAR(500) NULL,
    original_file_name VARCHAR(255) NULL,
    resume_text VARCHAR(5000) NULL,
    stored_file_path VARCHAR(500) NULL,
    title VARCHAR(100) NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_application_documents_user_id (user_id),
    CONSTRAINT fk_application_documents_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS job_postings (
    deadline DATE NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    updated_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    position_title VARCHAR(100) NOT NULL,
    job_url VARCHAR(300) NULL,
    file_url VARCHAR(500) NULL,
    description VARCHAR(5000) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_job_postings_user_id (user_id),
    CONSTRAINT fk_job_postings_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS interview_sessions (
    cover_letter_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    ended_at DATETIME(6) NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_posting_id BIGINT NULL,
    resume_id BIGINT NULL,
    started_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    position_title VARCHAR(100) NOT NULL,
    title VARCHAR(100) NOT NULL,
    cover_letter_snapshot TEXT NULL,
    job_posting_snapshot TEXT NULL,
    resume_snapshot TEXT NULL,
    status ENUM('COMPLETED', 'ONGOING') NOT NULL,
    application_document_id BIGINT NULL,
    application_document_snapshot TEXT NULL,
    PRIMARY KEY (id),
    KEY idx_interview_sessions_user_id (user_id),
    CONSTRAINT fk_interview_sessions_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS interview_questions (
    sequence_number INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    interview_session_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    question_text VARCHAR(1000) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_interview_questions_session_id (interview_session_id),
    CONSTRAINT fk_interview_questions_session FOREIGN KEY (interview_session_id) REFERENCES interview_sessions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS interview_answers (
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    interview_question_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    audio_url VARCHAR(500) NULL,
    answer_text VARCHAR(5000) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_interview_answers_question_id (interview_question_id),
    CONSTRAINT fk_interview_answers_question FOREIGN KEY (interview_question_id) REFERENCES interview_questions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS interview_feedback (
    logic_score INT NOT NULL,
    overall_score INT NOT NULL,
    relevance_score INT NOT NULL,
    specificity_score INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    interview_session_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    improvements VARCHAR(2000) NOT NULL,
    weak_points VARCHAR(2000) NOT NULL,
    recommended_answer VARCHAR(3000) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_interview_feedback_session_id (interview_session_id),
    CONSTRAINT fk_interview_feedback_session FOREIGN KEY (interview_session_id) REFERENCES interview_sessions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS books (
    price DECIMAL(12, 2) NOT NULL,
    stock INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    updated_at DATETIME(6) NOT NULL,
    author VARCHAR(100) NOT NULL,
    publisher VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    cover_url VARCHAR(500) NULL,
    description VARCHAR(5000) NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cart_items (
    quantity INT NOT NULL,
    book_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    updated_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_item_user_book (user_id, book_id),
    KEY idx_cart_items_book_id (book_id),
    CONSTRAINT fk_cart_items_book FOREIGN KEY (book_id) REFERENCES books (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS orders (
    total_price DECIMAL(12, 2) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    ordered_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    address VARCHAR(500) NOT NULL,
    status ENUM('CANCELLED', 'PAID', 'PENDING', 'SHIPPED') NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_items (
    price DECIMAL(12, 2) NOT NULL,
    quantity INT NOT NULL,
    book_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_order_items_book_id (book_id),
    KEY idx_order_items_order_id (order_id),
    CONSTRAINT fk_order_items_book FOREIGN KEY (book_id) REFERENCES books (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
