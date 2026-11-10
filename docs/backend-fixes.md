# Correções backend — retomada 10/11/2026

Responsável real: subagente `backend_fixes`, propriedade `backend/`. Código anterior preservado. Correções obrigatórias da Rodada A, sem consumir cota de melhorias opcionais.

- **S-A2:** `alertAllowed` centraliza verificação de vínculo atual, escopo de categoria, consentimento atual e preferências de aviso/plano. Aplicada ao criar, ler e entregar alertas. Retirada de consentimento oculta detalhes anteriores imediatamente e muda pendências para `revoked` na entrega; sincronização também filtra saúde/localização conforme consentimento, inclusive dono. Testes retiram consentimento de saúde e localização, removem escopo e desligam preferência.
- **S-A3:** confirmação `taken` reconcilia histórico com `acknowledgedAt` real e cancela entrega `pending/retry`. O entregador reconsulta confirmações antes de enviar qualquer aviso sem confirmação, cobrindo fila anteriormente não reconciliada. Teste registra confirmação offline tardia, cadastra assinatura posterior e demonstra zero tentativas do aviso cancelado.
- **S-A4:** saída detectada sem posição aceita somente payload explícito `{locationUnavailable:true}` sem latitude/longitude/accuracy/timestamp. Payload vazio ou misto é rejeitado. Contrato combinado com `android_fixes`; nenhum mapa/coordenada é inventado. Teste envia saída sem posição, replay idempotente e evento de bateria seguinte com sucesso.

## Testes executados

`cd /workspace/lembrebem/backend && npm test` em Node v24.22.1: **16 testes, 16 aprovados, 0 falhas**, incluindo 12 preexistentes e 4 regressões novas (a primeira percorre duas categorias). SQLite real via `node:sqlite`; API, scheduler e verificador RSA são código de produção. Os testes não dependem de rede, credenciais ou dados pessoais reais. `node --check` usado após correção de sintaxe no desenvolvimento; a suíte final importa os módulos com sucesso.

## Integração inspecionada

Android usa `/v1`, HTTPS, `Authorization: Bearer` Firebase ID token. Programações enviam `sourceReviewed:true`, versão e instantes de dose canônicos compatíveis. `timeZone` e literais clínicos coincidem no contrato. `locationUnavailable` coordenado com Android. A integração foi inspecionada por código e exercitada em API local; não equivale a integração em dois celulares nem chamada FCM real.

Nenhuma conta foi implantada, nenhuma credencial ou push real foi usado. FCM, Workers/D1 em nuvem e geofence físico ainda exigem configuração e teste pelo proprietário. Rodada B pertence aos revisores independentes e não é alegada neste relatório.
