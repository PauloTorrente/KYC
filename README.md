# KYC Básico — Cadastro P2P

Cadastro local de clientes (nome + CPF) para operação P2P. Validação de CPF
100% offline (dígito verificador), sem consulta a base externa.

## Stack
- Java 21 + Spring Boot 3.3
- PostgreSQL
- Maven

## Decisões de segurança (leia antes de usar)
- **CPF não é guardado em texto puro.** O banco grava o hash SHA-256 (com pepper)
  e uma versão mascarada (`123.***.***-09`) só para exibição. Isso permite
  detectar duplicados e buscar sem expor o número se a base vazar.
- **Pepper obrigatório.** Defina um segredo próprio via variável de ambiente
  `KYC_CPF_PEPPER` antes de rodar em sério. Se você trocar o pepper depois,
  os hashes antigos param de bater — escolha um e mantenha.
- **Auditoria mínima:** cada registro guarda `criado_em` e `registrado_por`.

## Como rodar

1. Suba um Postgres e crie o banco:
   ```sql
   CREATE DATABASE kyc;
   ```

2. Defina as variáveis (ou edite `application.properties`):
   ```bash
   export DB_USER=postgres
   export DB_PASSWORD=suasenha
   export KYC_CPF_PEPPER=algum-segredo-bem-aleatorio
   ```

3. Rode:
   ```bash
   mvn spring-boot:run
   ```
   A tabela `cliente` é criada automaticamente (ddl-auto=update).

## Endpoints

Cadastrar:
```bash
curl -X POST http://localhost:8080/api/clientes \
  -H "Content-Type: application/json" \
  -d '{"nome":"Fulano de Tal","cpf":"111.444.777-35","registradoPor":"paulo"}'
```

Listar (retorna CPF mascarado):
```bash
curl http://localhost:8080/api/clientes
```

Buscar por id:
```bash
curl http://localhost:8080/api/clientes/1
```

## Respostas de erro
- CPF com dígito inválido → 400
- CPF já cadastrado → 409
- Campo faltando → 400

## Limitações conscientes
- Este sistema valida apenas a **estrutura** do CPF. Ele NÃO confirma que o CPF
  existe na Receita nem que pertence à pessoa do nome informado. Para isso é
  preciso consulta a base oficial/antifraude (escopo maior, não incluído).
- Não há triagem de sanções/PEP nem limites por operação. Se a operação crescer
  ou o regulador exigir PLD/FT, este cadastro é o ponto de partida, não a solução
  completa.
