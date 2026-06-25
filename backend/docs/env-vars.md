# Variáveis de Ambiente — Produção

| Variável | Obrigatória | Descrição | Exemplo |
|----------|-------------|-----------|---------|
| `SPRING_PROFILES_ACTIVE` | Sim | Perfil Spring ativo | `prod` |
| `DB_URL` | Sim | JDBC URL do MySQL | `jdbc:mysql://host:3306/batalhanaval?useSSL=true` |
| `DB_USERNAME` | Sim | Usuário do banco | `batalhanaval_user` |
| `DB_PASSWORD` | Sim | Senha do banco | — |
| `JWT_SECRET` | Sim | Chave HMAC-SHA256 (mínimo 32 caracteres) | — |
| `CORS_ALLOWED_ORIGINS` | Sim | Origens permitidas (comma-separated) | `https://batalhanaval.vercel.app` |
| `PORT` | Não | Porta do servidor (default: 8080) | `8080` |
