-- Tabela de usuários do sistema
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL
);

-- Tabela de motoristas da frota
CREATE TABLE drivers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    cpf VARCHAR(14) UNIQUE NOT NULL,
    cnh VARCHAR(20) UNIQUE NOT NULL,
    cnh_category VARCHAR(5),
    cnh_expiration DATE,
    phone VARCHAR(20),
    email VARCHAR(100),
    status ENUM('ATIVO', 'INATIVO') NOT NULL DEFAULT 'ATIVO',
    deleted_at DATETIME NULL
);

-- Tabela de veículos
CREATE TABLE vehicles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    plate VARCHAR(10) UNIQUE NOT NULL,
    model VARCHAR(100) NOT NULL,
    brand VARCHAR(100),
    year INT,
    status ENUM('ATIVO', 'INATIVO', 'MANUTENCAO') DEFAULT 'ATIVO',
    default_driver_id INT NULL COMMENT 'Motorista padrao/principal do veiculo',
    deleted_at DATETIME NULL,

    FOREIGN KEY (default_driver_id) REFERENCES drivers(id) ON DELETE SET NULL,
    INDEX idx_vehicles_default_driver (default_driver_id)
);

-- ✅ MOVER gps_devices PARA CÁ (antes de trips)
CREATE TABLE gps_devices (
    id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id INT NULL,
    imei VARCHAR(50) UNIQUE NOT NULL,
    latitude DECIMAL(9,6) DEFAULT 0,
    longitude DECIMAL(9,6) DEFAULT 0,
    date_time DATETIME,
    speed DECIMAL(5,2) DEFAULT 0,
    heading DECIMAL(5,2) DEFAULT 0,
    icon_map_url VARCHAR(255),
    title VARCHAR(255),
    ignition BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE SET NULL
);

-- Tabela de viagens (agora gps_devices já existe)
CREATE TABLE trips (
    id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id INT NOT NULL,
    driver_id INT NULL COMMENT 'Motorista responsavel (pode ser NULL se nao identificado)',
    start_location VARCHAR(255),
    end_location VARCHAR(255),
    start_latitude DECIMAL(10, 8) NULL COMMENT 'Latitude do ponto de inicio',
    start_longitude DECIMAL(11, 8) NULL COMMENT 'Longitude do ponto de inicio',
    end_latitude DECIMAL(10, 8) NULL COMMENT 'Latitude do ponto de fim',
    end_longitude DECIMAL(11, 8) NULL COMMENT 'Longitude do ponto de fim',
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    distance_km DECIMAL(10,2),
    auto_generated BOOLEAN DEFAULT FALSE COMMENT 'Viagem criada automaticamente pelo GPS',
    gps_device_id INT NULL COMMENT 'Dispositivo GPS que gerou a viagem',
    max_speed_kmh DECIMAL(5, 2) NULL COMMENT 'Velocidade maxima atingida (km/h)',
    avg_speed_kmh DECIMAL(5, 2) NULL COMMENT 'Velocidade media (km/h)',
    status ENUM('PLANEJADA', 'EM_ANDAMENTO', 'CONCLUIDA', 'CANCELADA') DEFAULT 'PLANEJADA',

    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    FOREIGN KEY (driver_id) REFERENCES drivers(id),
    FOREIGN KEY (gps_device_id) REFERENCES gps_devices(id) ON DELETE SET NULL,
    INDEX idx_trips_auto_generated (auto_generated),
    INDEX idx_trips_gps_device (gps_device_id),
    INDEX idx_trips_start_time (start_time)
);

-- Tabela de despesas gerais relacionadas à frota
CREATE TABLE expenses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id INT,
    driver_id INT,
    trip_id INT,
    date DATE NOT NULL,
    type VARCHAR(50) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    description TEXT,
    liters DECIMAL(10,2),
    price_per_liter DECIMAL(10,2),
    odometer INT,

    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    FOREIGN KEY (driver_id) REFERENCES drivers(id),
    FOREIGN KEY (trip_id) REFERENCES trips(id)
);

-- Histórico de posições GPS
CREATE TABLE gps_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    gps_device_id INT NOT NULL,
    vehicle_id INT NULL,
    date_time DATETIME NOT NULL,
    speed DECIMAL(5,2) DEFAULT 0,
    latitude DECIMAL(9,6) NOT NULL,
    longitude DECIMAL(9,6) NOT NULL,
    raw_log TEXT NOT NULL,

    FOREIGN KEY (gps_device_id) REFERENCES gps_devices(id) ON DELETE CASCADE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE SET NULL,

    INDEX idx_device_time (gps_device_id, date_time),
    INDEX idx_datetime (date_time),
    INDEX idx_vehicle_time (vehicle_id, date_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;