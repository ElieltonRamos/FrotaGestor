-- Tabela de usuários do sistema
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,         -- Identificador único de cada usuário
    username VARCHAR(50) UNIQUE NOT NULL,      -- Nome de login (deve ser único)
    password VARCHAR(255) NOT NULL,            -- Senha (armazenada de forma criptografada)
    role VARCHAR(20) NOT NULL                  -- Papel do usuário (ex.: admin, gestor, motorista)
);

-- Tabela de motoristas da frota
CREATE TABLE drivers (
    id INT AUTO_INCREMENT PRIMARY KEY,                          -- Identificador único do motorista
    name VARCHAR(100) NOT NULL,                                 -- Nome completo do motorista
    cpf VARCHAR(14) UNIQUE NOT NULL,                            -- CPF (deve ser único)
    cnh VARCHAR(20) UNIQUE NOT NULL,                            -- Número da CNH (deve ser único)
    cnh_category VARCHAR(5),                                    -- Categoria da CNH (ex.: B, C, D, E)
    cnh_expiration DATE,                                        -- Data de validade da CNH
    phone VARCHAR(20),                                          -- Telefone do motorista
    email VARCHAR(100),                                         -- E-mail do motorista
    status ENUM('ATIVO', 'INATIVO') NOT NULL DEFAULT 'ATIVO', -- Situação do motorista
    deleted_at DATETIME NULL
);

-- Tabela de veículos da frota
CREATE TABLE vehicles (
    id INT AUTO_INCREMENT PRIMARY KEY,                                         -- Identificador único do veículo
    plate VARCHAR(10) UNIQUE NOT NULL,                                         -- Placa do veículo (única)
    model VARCHAR(100) NOT NULL,                                               -- Modelo do veículo
    brand VARCHAR(100),                                                        -- Marca/fabricante do veículo
    year INT,                                                                  -- Ano de fabricação
    status ENUM('ATIVO', 'INATIVO', 'MANUTENCAO') DEFAULT 'ATIVO',          -- Situação do veículo
    deleted_at DATETIME NULL
);

-- Tabela de viagens/rotas realizadas
CREATE TABLE trips (
    id INT AUTO_INCREMENT PRIMARY KEY,                                         -- Identificador único da viagem
    vehicle_id INT NOT NULL,                                                   -- Referência ao veículo utilizado
    driver_id INT NOT NULL,                                                    -- Referência ao motorista responsável
    start_location VARCHAR(255),                                               -- Local de início da viagem
    end_location VARCHAR(255),                                                 -- Local de destino da viagem
    start_time DATETIME NOT NULL,                                              -- Data e hora de início
    end_time DATETIME,                                                         -- Data e hora de término
    distance_km DECIMAL(10,2),                                                 -- Distância percorrida em km
    status ENUM('PLANEJADA', 'EM_ANDAMENTO', 'CONCLUIDA', 'CANCELADA') DEFAULT 'PLANEJADA', -- Status da viagem
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),                          -- Relacionamento com veículos
    FOREIGN KEY (driver_id) REFERENCES drivers(id)                             -- Relacionamento com motoristas
);

-- Tabela de despesas gerais relacionadas à frota
CREATE TABLE expenses (
    id INT AUTO_INCREMENT PRIMARY KEY,       -- Identificador único da despesa
    vehicle_id INT,                          -- Veículo relacionado à despesa (opcional)
    driver_id INT,                           -- Motorista relacionado à despesa (opcional)
    trip_id INT,                             -- Viagem relacionada à despesa (opcional)
    date DATE NOT NULL,                      -- Data da despesa
    type VARCHAR(50) NOT NULL,               -- Tipo da despesa (ex.: Pedágio, Estacionamento, Lavagem, Outro)
    amount DECIMAL(10,2) NOT NULL,           -- Valor da despesa
    description TEXT,                        -- Descrição detalhada da despesa
    liters DECIMAL(10,2),                        -- Quantidade de litros abastecidos
    price_per_liter DECIMAL(10,2),               -- Preço por litro de combustível
    odometer INT,                                -- Quilometragem do veículo no momento do abastecimento

    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id), -- Relacionamento com veículos
    FOREIGN KEY (driver_id) REFERENCES drivers(id),   -- Relacionamento com motoristas
    FOREIGN KEY (trip_id) REFERENCES trips(id)        -- Relacionamento com viagens
);

CREATE TABLE gps_devices (
    id INT AUTO_INCREMENT PRIMARY KEY,        -- ID do dispositivo
    vehicle_id INT NULL,                      -- Veículo vinculado (AGORA PODE SER NULL)
    imei VARCHAR(50) UNIQUE NOT NULL,         -- Identificador do GPS
    latitude DECIMAL(9,6) DEFAULT 0,          -- Última latitude
    longitude DECIMAL(9,6) DEFAULT 0,         -- Última longitude
    date_time DATETIME,                       -- Momento da leitura
    speed DECIMAL(5,2) DEFAULT 0,             -- Velocidade (opcional)
    heading DECIMAL(5,2) DEFAULT 0,           -- Direção (opcional)
    icon_map_url VARCHAR(255),                -- Ícone para o mapa
    title VARCHAR(255),                       -- Modelo + placa
    ignition BOOLEAN DEFAULT FALSE,           -- Ignition ligada/desligada
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE SET NULL
);