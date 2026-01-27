# FrotaGestor

Sistema completo de **gestão de frotas** com rastreamento GPS em tempo real, relatórios avançados e automação de processos operacionais.

- **Backend:** Kotlin + Ktor  
- **Frontend:** Angular  
- **Integração GPS:** TCP (Suntech ST300/ST310, GT-06)  
- **Versão atual:** `v1.1.0-prod`

---

## 🚀 Recursos Principais

- **Rastreamento GPS em tempo real** via TCP, com parsing de protocolos **Suntech** e **GT-06**  
  - Histórico detalhado com velocidade, alertas e nível de bateria  
- **CRUD completo**:
  - Veículos
  - Motoristas
  - Viagens
  - Despesas
  - Abastecimentos
  - Manutenções
- **Dashboard de monitoramento**:
  - Métricas operacionais
  - Status do sistema
  - Mapa interativo com **MapLibre GL**
- **Relatórios avançados**:
  - Veículos (ativos / em manutenção)
  - Motoristas
  - Viagens
  - Despesas  
  - Filtros por data, motorista e veículo
- **Sub-frotas hierárquicas**
- Paginação, filtros e ordenação em todas as listagens
- **Backup automático MySQL** (`mysqldump + JDBC`)
- **Comandos remotos para dispositivos GPS**:
  - Localização
  - Imobilizador
  - Reboot
- **Segurança e qualidade**:
  - Autenticação JWT
  - Soft-delete
  - Validações
  - Indicadores e métricas agregadas

---

## 🧰 Tecnologias

### Frontend
- Angular 20+ (TypeScript)
- Tailwind CSS (UI responsiva)
- MapLibre GL (mapas)
- Ng-Icons (Heroicons)
- JWT-Decode

### Backend
- Kotlin + Ktor (API / servidor web)
- Exposed (SQL DSL)
- HikariCP (pool de conexões)
- MySQL
- kotlinx-datetime
- kotlinx-serialization

### Protocolos GPS
- Suntech ST300 / ST310
- GT-06  
- Comunicação TCP dedicada

---

## 🔌 APIs e Integrações

- **Servidores TCP dedicados**:
  - Porta `5023` (Suntech)
  - Porta `3003` (GT-06)
- **Comandos GPS suportados**:
  - `requestLocation`
  - `immobilizer`
  - `setIntervals`
  - `reboot`
- **Relatórios agregados** com `GROUP BY` e filtros dinâmicos

---

## 📋 Pré-requisitos

- **JDK 17+** (Kotlin / Gradle)
- **Node.js 18+** e npm ou yarn
- **MySQL 8.0+**
- Plataformas suportadas:
  - Windows x86-64
  - Linux (DEB/RPM via `jpackage`)

---

## ⚡ Instalação Rápida

```bash
# Backend
git clone <repo-url> frotagestor
cd backend
./gradlew run
# ou configure o application.conf para MySQL

# Frontend (diretório paralelo)
cd ../frontend
npm install
ng serve
# http://localhost:4200 (ajuste API_URL para backend)
```

> Para produção:
```bash
./gradlew jpackage
```
Gera executáveis nativos para Windows/Linux.

---

## 🛠️ Comandos de Build

| Comando | Descrição | Saída |
|------|---------|------|
| `./gradlew run` | Executa backend (dev) | Ktor na porta 3001 |
| `./gradlew build` | Build do JAR | `build/libs/frotagestor.jar` |
| `npm run start` | Frontend (dev) | `http://localhost:4200` |
| `./gradlew jpackage` | Build desktop nativo | `build/image/bin/frotagestor` |
| `./gradlew backupExecute` | Backup manual do DB | `backups/backup.sql` |

---

## 🗂️ Estrutura do Projeto

```text
frotagestor/
├── backend/
│   ├── src/main/kotlin/    # Controllers, Services, TCP Servers
│   ├── build.gradle.kts
│   └── application.conf
├── frontend/
│   ├── src/app/            # vehicles, drivers, dashboard...
│   ├── package.json
│   └── tailwind.config.js
├── database/
│   └── migrations/
│       └── V1__init_schema.sql
└── README.md
```

- Banco MySQL criado via **migrations**
- Backups armazenados em `backups/`

---

## 🧪 Desenvolvimento

```bash
# Backend (hot-reload)
./gradlew run --continuous

# Frontend
npm run lint
npm run format
```

> Para testes locais, configure `application-local.conf` com MySQL localhost.

### Linux
Dependências necessárias para build:
```bash
sudo apt install libwebkit2gtk-dev libssl-dev
```

---

## 🌐 Endpoints Principais

- `/vehicles/report?startDate=...&endDate=...` — Relatórios veiculares  
- `/gps/command/{imei}?type=location` — Comandos GPS  
- `/backup/execute` — Backup manual do banco  
- `/monitoring/status` — Métricas do dashboard  

---

## 🎯 Público-Alvo

- Empresas de logística  
- Frotas corporativas  
- Operações de rastreamento veicular com integração a hardware GPS

---

## 📄 Licença

Projeto **privado / open-source** sob licença **MIT**.  
Para uso comercial, entre em contato com o desenvolvedor.

---

## 🤝 Contribuições

1. Fork o repositório  
2. Crie uma branch: `feat/minha-feature`  
3. Commit suas alterações  
4. Abra um PR para `develop`  
5. Execute os testes:
   ```bash
   ./gradlew test
   ```

---

## 🛣️ Roadmap

- Relatórios em PDF / Excel
- Multi-tenant
- App mobile (Kotlin Multiplatform)
- Integração em nuvem (AWS S3 para backups)
- Suporte a mais protocolos GPS

---

Desenvolvido para **gestão inteligente de frotas**.  
Monitore, analise e otimize sua operação!
