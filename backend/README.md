# API LembreBem

Backend executável Cloudflare Workers + D1. Node >=24 para demonstração e testes SQLite; Workers usa D1 nativo. Nenhum áudio ou imagem é aceito pelas rotas. Modo push `demo` grava `delivery=demo`; isso NÃO envia notificações reais.

## Rodar gratuitamente em desenvolvimento

```sh
npm test
npm run demo
```

Servidor somente `127.0.0.1:8787`, arquivo `demo.sqlite`, identidade fictícia pelo cabeçalho `x-demo-user: familiar-demo`. O Worker de produção não interpreta esse cabeçalho, nem aceita qualquer token de demonstração. Nunca publicar o servidor demo na internet. `PORT` e `DEMO_DB` são opcionais. O APK distribuível exige HTTPS, portanto use endpoint Workers para teste remoto; demo loopback destina-se aos testes no computador. Não incluímos credenciais nem implantamos contas.

## Configuração real pelo proprietário

1. Criar Firebase projeto, habilitar Authentication email/senha e FCM; registrar aplicativo Android e fornecer configuração pública Firebase. Não exige autenticação telefônica.
2. Criar D1: `npx wrangler d1 create lembrebem`, substituir database_id em `wrangler.toml` e FIREBASE_PROJECT_ID.
3. `npm install`, `npm run migrate:remote` (altera apenas banco que o proprietário configurou).
4. Gerar credencial de conta de serviço com permissão restrita de FCM. `npx wrangler secret put FCM_SERVICE_ACCOUNT_JSON` recebe JSON pelo terminal; nunca colocar JSON no APK/repositório.
5. Mudar `PUSH_MODE` para `fcm`; implantar somente após revisão/autorização do proprietário: `npm run deploy`.
6. Registrar token FCM individual via `/v1/subscriptions`. Credencial privada fica exclusivamente no Worker; token de dispositivo é dado de roteamento e não deve ser logado.

FCM é gratuito e não garante entrega imediata. Cloudflare Workers Free/D1 têm quotas; consultar limites oficiais antes do piloto. A arquitetura usa cron por minuto e pagina envio de até 100 alertas por rodada; isso não é garantia de vigilância. Sem conta configurada, app mantém funções locais.

## Contrato HTTP v1

Todas as rotas `/v1` exigem `Authorization: Bearer <Firebase ID token>` em produção. JWT RS256 exige assinatura Google, audience/issuer projeto correto, subject, exp, iat e auth_time. Cache de JWKS 5min. TLS é fornecido pelo Workers. Respostas sem cache; erros não incluem conteúdo recebido. Limite 120 requisições/usuário/minuto e corpo 64KiB. Falhas: 400 validação, 401 autenticação, 403 autorização, 404 rota, 409 conflito, 429 limite, 503 indisponibilidade.

| Método e rota | Corpo / resultado |
|---|---|
| GET `/health` | estado `pushMode`, sem dados pessoais |
| GET `/v1/people` | IDs dos perfis próprios ou vinculados |
| POST `/v1/people` | `{id:UUID,preferredLocale,countryCode,timeZone,consents:{health:boolean,location:boolean}}`; idempotente mesmo dono |
| GET `/v1/people/:id` | perfil mínimo, consentimentos, último heartbeat |
| PUT `/v1/people/:id` | campos perfil + `heartbeatMinutes` 30..10080; só dono |
| DELETE `/v1/people/:id` | exclusão cascata; só dono |
| POST `/v1/people/:id/invites` | `{scopes:[read,health,location,alerts],consentConfirmed:true}`; só dono; retorna código único 24h |
| POST `/v1/invites/accept` | `{code,preferredLocale}`; consumo atômico único |
| GET `/v1/people/:id/grants` | vínculos; só dono |
| DELETE `/v1/people/:id/grants/:uid` | revogação imediata e remoção dos alertas do vínculo |
| PUT `/v1/people/:id/grants` | `{preferredLocale,preferences:{dose:false,unconfirmed:false,"plan:UUID":false}}`; só próprio vínculo |
| POST `/v1/people/:id/plans` | `{id,version,approved:true,sourceReviewed:true,name,dosage,time:"08:00",days:[1..7],startDate:"YYYY-MM-DD",endDate?,timeZone}`; escopo health, consentimento; revisão humana obrigatória |
| GET `/v1/people/:id/plans` | programações; escopo health |
| POST `/v1/people/:id/events` | `{id:UUID,type,occurredAt:UTC,payload}`; idempotência global e conflito se ID reaproveitado com outro conteúdo |
| POST `/v1/people/:id/heartbeat` | `{occurredAt:UTC}`; último instante monotônico |
| GET `/v1/people/:id/sync?since=UTC` | planos e eventos desde recebimento, máximo 500; escopos filtram saúde/localização |
| GET `/v1/people/:id/alerts` | somente alertas do próprio usuário, máximo 200 |
| GET `/v1/people/:id/export` | exportação pessoa/planos/eventos/auditoria; só dono |
| POST `/v1/subscriptions` | `{token,preferredLocale}`; token apenas do próprio usuário |

Eventos: `dose_ack` exige `{doseId,status:"taken"|"not_yet"|"snoozed"}`; doseId é `planId:scheduledAtISO` (ex.: ISO inclui `.000Z`). `battery` exige `{level:0..100,charging:boolean}`. `geofence_exit` exige `{latitude,longitude,accuracy,timestamp:UTC}` (aceita alias `locationAt`) ou explicitamente `{locationUnavailable:true}` sem nenhum campo de localização; ambos exigem consentimento e escopo location. A ausência de posição não inventa coordenadas nem impede o próximo evento. `help`, `connectivity`, `appointment` transportam estado mínimo consentido. Locais usados em teste são fictícios (0,0).

## Regras e armazenamento

- UUIDs; UTC transporte, IANA no plano. Dias ISO segunda=1, domingo=7. Conteúdo clínico literal, nunca traduzido. Programações versionadas (próxima versão = anterior+1), revisão e ator auditados.
- Cron avalia 30 minutos de recuperação por execução; avisa todos vínculos autorizados com preferência ativa. No horário e após 15min sem confirmação. Mensagem nunca afirma dose omitida. O cron não garante exatidão durante indisponibilidade da plataforma. D1 armazena mínimo autorizado e não armazena gravações/documentos.
- Confirmação tardia preserva horário real e recebimento, anota reconciliação de alerta anterior sem falsificar histórico. Push contém só título genérico, texto privado e ID para buscar detalhe autenticado. Cliente deve deduplicar por ID: entrega de rede é ao menos uma tentativa, não exatamente uma vez.
- Bateria alerta em cruzamentos 30/15 e início real de carregamento reportado pelo Android. Sem interpretação de botão como carregamento.
- Heartbeat padrão 120min, ajustável 30..10080. Texto neutro com último horário conhecido. Não afirma causa de desconexão.
- Migração `0001_initial.sql` tem índices por pessoa/recebimento e entrega. Eventos e alertas expiram 90 dias pelo cron; convites expirados são removidos; limits expiram 10min. Planos/auditoria persistem até exclusão do perfil. Áudios/imagens permanecem exclusivamente locais.
- Tokens FCM expirados 404/410 removidos. Falhas tentam até 8 rodadas com backoff; estado failed visível via API. Alerta sem token fica pendente. Revogação, consentimento de categoria e preferências são revalidados antes de envio e leitura de alertas anteriores. Confirmação tardia cancela o envio pendente/retry de aviso sem confirmação; o histórico mantém a reconciliação.

## Limites verificados

Testes usam SQLite real via `node:sqlite` e a mesma API/scheduler do Worker. Não validam contas Firebase/FCM/D1 reais nem entrega em celular. Não houve implantação. Traduções iniciais nos cinco idiomas exigem revisão humana de saúde/segurança e jurídica antes de uso público. Consumo e limites gratuitos precisam ser medidos no piloto; não se promete custo zero em escala ilimitada.

## Correções de integração da rodada B

Telemetria do dispositivo assistido (`heartbeat`, `battery`, `help`, `connectivity`, `geofence_exit`) só é aceita da identidade proprietária do perfil. Familiares vinculados podem consultar conforme escopo, mas não simular o estado do aparelho. Confirmações de dose usam o último evento por `occurredAt`, com desempate por recebimento e ordem de inserção; correções humanas preservam todos os eventos anteriores. Um `taken` antigo sincronizado depois não sobrescreve uma correção mais recente.

`GET /v1/people/:id/sync?cursor=<nextCursor>` pagina eventos em ordem estável `(receivedAt,id)`, até 500 por página. A resposta contém `nextCursor` e `hasMore`; persistir o cursor somente após aplicar a página localmente e continuar enquanto `hasMore=true`. O cursor é opaco e não substitui autenticação. Eventos filtrados por consentimento/escopo também avançam o cursor; se permissões mudarem e for preciso recuperar histórico anteriormente não autorizado, reinicie a sincronização sem cursor. `since=UTC` continua disponível para compatibilidade. As programações continuam incluídas independentemente da paginação de eventos, respeitando consentimento.

Eventos `appointment` exigem `payload.id` UUID, `title` não vazio (até 200 caracteres), `at` UTC e campos opcionais `place`, `preparation`, `documents` textuais até 4000 caracteres. Payloads incompletos são rejeitados antes de entrar na fila.
