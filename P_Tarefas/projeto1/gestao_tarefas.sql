-- ============================================================================
-- Base de Dados: Gestão de Tarefas
-- Compatível com os Servlets Java (Tomcat), JavaFX e com o Modelo ER completo
-- ============================================================================

DROP DATABASE IF EXISTS gestao_de_tarefas_base_dados;

CREATE DATABASE gestao_de_tarefas_base_dados
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE gestao_de_tarefas_base_dados;

-- 1. Utilizador da aplicação no MySQL (usado pelo Db.java)
CREATE USER IF NOT EXISTS 'app_tarefas'@'localhost' IDENTIFIED BY 'App#2026pass';
ALTER USER 'app_tarefas'@'localhost' IDENTIFIED BY 'App#2026pass';
GRANT SELECT, INSERT, UPDATE, DELETE ON gestao_de_tarefas_base_dados.* TO 'app_tarefas'@'localhost';
FLUSH PRIVILEGES;

-- 2. Tabela: Utilizadores
CREATE TABLE utilizadores (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    nome VARCHAR(50),
    telefone VARCHAR(15),
    email VARCHAR(120),
    password_hash VARCHAR(255) NOT NULL,
    papel ENUM('UTILIZADOR','ADMIN') NOT NULL DEFAULT 'UTILIZADOR',
    nivel TINYINT NOT NULL DEFAULT 1,
    data_registo TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(20) DEFAULT 'ATIVO'
);

-- 3. Tabela: Categorias
CREATE TABLE categorias (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(50) NOT NULL,
    descricao VARCHAR(255)
);

-- 4. Tabela: Prioridades
CREATE TABLE prioridades (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(30) NOT NULL,
    nivel TINYINT NOT NULL
);

-- 5. Tabela: Tarefas
CREATE TABLE tarefas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utilizador_id INT NOT NULL,
    categoria_id INT NULL,
    prioridade_id INT NULL,
    titulo VARCHAR(100) NOT NULL,
    descricao TEXT,
    concluida BOOLEAN NOT NULL DEFAULT FALSE,
    criada_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deadline TIMESTAMP NULL,
    FOREIGN KEY (utilizador_id) REFERENCES utilizadores(id) ON DELETE CASCADE,
    FOREIGN KEY (categoria_id) REFERENCES categorias(id) ON DELETE SET NULL,
    FOREIGN KEY (prioridade_id) REFERENCES prioridades(id) ON DELETE SET NULL
);

-- 6. Tabela: Tokens de Autenticação
CREATE TABLE tokens_autenticacao (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utilizador_id INT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expira_em DATETIME NOT NULL,
    FOREIGN KEY (utilizador_id) REFERENCES utilizadores(id) ON DELETE CASCADE
);

-- 7. Categorias e Prioridades Iniciais (Exemplo)
INSERT INTO categorias (nome, descricao) VALUES 
('Trabalho', 'Tarefas profissionais e académicas'),
('Pessoal', 'Tarefas do dia a dia');

INSERT INTO prioridades (nome, nivel) VALUES 
('Alta', 1),
('Média', 2),
('Baixa', 3);

-- 8. Utilizadores Iniciais (Palavra-passe: "admin" para admin e "eduardo" para eduardo)
INSERT INTO utilizadores (username, nome, email, password_hash, papel, estado) VALUES
('admin', 'Administrador', 'admin@isep.ipp.pt', '120000:K5ihTSzAe7J6/Gbn6q9Scw==:Og03QWL0VL9f3iB34vc63MI02/i/ZqtySIugKNhiT1s=', 'ADMIN', 'ATIVO'),
('eduardo', 'Eduardo', 'eduardo@isep.ipp.pt', '120000:c2lggEZQnKxw480P6nmT5g==:uJT9BjV+7FAKQNj+CyfET3rjlW0LEZ6CgvrBNZUUn7Q=', 'UTILIZADOR', 'ATIVO');
