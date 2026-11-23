-- Reiniciar a Base de Dados do zero (para apagar tabelas antigas)
DROP DATABASE IF EXISTS gestao_de_tarefas_base_dados;

CREATE DATABASE gestao_de_tarefas_base_dados
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE gestao_de_tarefas_base_dados;

-- 1. Tabela: Utilizadores
CREATE TABLE utilizadores (
    id_utilizadores INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(50) NOT NULL,
    telefone VARCHAR(15),
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nivel TINYINT NOT NULL DEFAULT 1,
    data_registo TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(20) DEFAULT 'PENDENTE'
);

-- 2. Tabela: Categorias
CREATE TABLE categorias (
    id_categorias INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(50) NOT NULL,
    descricao VARCHAR(255)
);

-- 3. Tabela: Prioridades
CREATE TABLE prioridades (
    id_prioridades INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(30) NOT NULL,
    nivel TINYINT NOT NULL
);

-- 4. Tabela: Tarefas
CREATE TABLE tarefas (
    id_tarefas INT AUTO_INCREMENT PRIMARY KEY,
    utilizadores_id INT NOT NULL,
    categorias_id INT,
    prioridades_id INT,
    titulo VARCHAR(100) NOT NULL,
    descricao TEXT,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    concluida BOOLEAN NOT NULL DEFAULT FALSE,
    deadline TIMESTAMP NULL,
    FOREIGN KEY (utilizadores_id) REFERENCES utilizadores(id_utilizadores) ON DELETE CASCADE,
    FOREIGN KEY (categorias_id) REFERENCES categorias(id_categorias) ON DELETE SET NULL,
    FOREIGN KEY (prioridades_id) REFERENCES prioridades(id_prioridades) ON DELETE SET NULL
);

-- 5. Tabela: Tokens de Autenticação
CREATE TABLE tokens_autenticacao (
    id_autenticacao INT AUTO_INCREMENT PRIMARY KEY,
    utilizador_id INT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expira_em DATETIME NOT NULL,
    FOREIGN KEY (utilizador_id) REFERENCES utilizadores(id_utilizadores) ON DELETE CASCADE
);

-- Criar utilizador de aplicação para o projeto JavaFX (opcional)
CREATE USER IF NOT EXISTS 'app_tarefas'@'localhost' IDENTIFIED BY 'senha_segura';
GRANT SELECT, INSERT, UPDATE, DELETE ON gestao_de_tarefas_base_dados.* TO 'app_tarefas'@'localhost';
FLUSH PRIVILEGES;
