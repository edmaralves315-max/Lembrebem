# LembreBem — Comando mestre de implementação para Codex

**Documento de referência do produto e instrução executável de engenharia.**
Data: 09/11/2026 · Versão da especificação: 1.2 (subagentes e melhoria contínua) · Idioma padrão: português do Brasil · Idiomas iniciais previstos: pt-BR, pt-PT, en-US, ja-JP, es-ES (arquitetura extensível) · Plataforma primária: Android · Situação: especificação para implementação, não software já validado.

## Instrução principal ao Codex

Você é o engenheiro principal de um produto Android **real**, chamado **LembreBem — Assistente de Memória, Cuidado e Segurança**. **Implemente em uma única entrega integrada o repositório completo**, incluindo aplicativo Android, API/backend, banco de dados, testes automatizados, dados fictícios de demonstração, documentação de instalação e configuração, e instruções para gerar APK de testes. Não entregue só mockups, telas estáticas, esqueleto vazio, protótipos que dependem de plataformas pagas ou pseudocódigo. Não separe a entrega em uma série de encomendas ao usuário; você pode fazer commits e testes incrementalmente **internamente**, mas o escopo e a arquitetura abrangem todos os módulos desde o começo. Se alguma função depende de uma credencial, configuração de console ou recurso indisponível no ambiente de desenvolvimento, **implemente sua interface e lógica real**, forneça um modo de demonstração claramente identificado e documente o passo manual, sem fingir que o serviço está operacional.

Use padrões atuais de Android, documentações oficiais e dependências estáveis compatíveis. Verifique licenças e mudanças recentes. Não execute nem implante o sistema em contas do usuário sem autorização explícita. Não inclua nomes, telefones, localizações, receitas ou dados médicos reais em exemplos, logs ou código.

## 0A. Orquestração obrigatória com subagentes e melhoria contínua controlada — RF-15

**Mandato:** sempre que o ambiente do Codex disponibilizar colaboração/multiagentes, o agente principal **DEVE delegar efetivamente** tarefas independentes a subagentes e consolidar seus resultados; não basta escrever que houve delegação. Use as ferramentas reais que o ambiente disponibilizar, sem inventar chamadas, nomes de ferramentas ou habilitar capacidades inexistentes. **Limite sugerido: quatro subagentes executando simultaneamente**, ajustável para menos por restrições do ambiente, custo/limites de utilização ou dependência entre tarefas. Se a superfície do Codex não oferecer subagentes, executar o mesmo plano por especialidades sequenciais e registrar a limitação em `IMPLEMENTATION_REPORT.md`, sem alegar ter empregado agentes paralelos. Não solicitar que o usuário programe o aplicativo parte por parte: esta é uma **única missão de implementação integrada**.

### Distribuição de responsabilidades

- **Agente principal (arquiteto/coordenador):** define contratos, estrutura do repositório, interfaces entre módulos, plano de integração, critérios de aceite, divisão de arquivos, prioridades e decisões finais. Integra contribuições, resolve incompatibilidades e nunca terceiriza a responsabilidade por segurança/entrega.
- **Subagente Android e acessibilidade:** Kotlin/Compose, Room, alarmes e TTS offline, fluxos de idoso e familiar, suporte multilíngue, permissões e acessibilidade.
- **Subagente backend, notificações e sincronização:** Cloudflare Workers/D1, autenticação e autorização, FCM, geofencing e eventos sincronizados, controles de privacidade, políticas de deduplicação e testes da API.
- **Subagente voz, documentos e IA opcional:** OCR local, gravação consentida, transcrição por interface substituível, rascunhos de resumo e revisão humana antes de qualquer alteração médica.
- **Subagente revisor de melhoria 1 — Usabilidade humana:** examina criticamente acessibilidade, facilidade para idosos, linguagem falada, redução de toques, nomes+fotos e familiares. Faz análise independente **preferencialmente somente leitura**, entregando sugestões verificáveis e pequenos testes de aceitação.
- **Subagente revisor de melhoria 2 — Segurança e confiabilidade:** examina riscos clínicos, privacidade, consentimento, localização, perda de internet, duplicidade de push, bateria, comportamento offline, internacionalização, permissões e exposição de dados. Faz análise independente **preferencialmente somente leitura**, entregando sugestões verificáveis e testes de falha.

Os agentes de implementação podem trabalhar em paralelo quando seus módulos e arquivos forem independentes. **Antes de delegar, registrar propriedade de pastas/arquivos e contratos de dados para impedir edição concorrente**. Agentes revisores não devem editar arquivos de produção simultaneamente com implementadores: entregam achados ao coordenador, que atribui correções a um único responsável. Se a superfície oferecer worktrees/branches, usá-los para alterações isoladas. Subagentes não terão acesso a chaves, dados pessoais ou credenciais reais; usar apenas dados fictícios.

### Processo obrigatório de descoberta de melhorias

**Executar duas rodadas de revisão orientada a evidências:**

1. **Rodada A, após o primeiro caminho integrado executável:** os dois revisores avaliam os fluxos completos `medicamento→confirmação→alerta familiar`, `saída de área segura→alerta→apoio familiar`, `bateria/conectividade`, `consulta gravada→rascunho→revisão`, `idiomas`, inclusive com serviços simulados. Identificar **até cinco sugestões por revisor**, com prioridade, problema observado, proposta, impacto, esforço, dependências, risco e teste verificável.
2. **Rodada B, antes da entrega:** repetir a revisão sobre a integração final, buscando regressões e melhorias comprovadas; não iniciar uma lista ilimitada de novos requisitos. Comparar com a Rodada A e registrar quais propostas realmente melhoraram indicadores ou testes.

Os revisores **devem procurar melhorias por iniciativa própria** e consultar documentação oficial pertinente quando houver pesquisa externa disponível. Considerar no mínimo: quantidade de interações para pedir ajuda, legibilidade e qualidade do áudio, alarmes com o celular bloqueado, falhas de internet, permissões negadas, consumo de bateria, privacidade de notificações, proteção contra dados clínicos errados, formatos multilíngues e condições de emergência. Podem propor reduzir complexidade ou remover fluxos confusos, não apenas adicionar funções.

### Orçamento e limites das melhorias

- Prioridade absoluta: implementar, integrar e testar os requisitos existentes RF-01 a RF-14. **Não sacrificar cobertura do escopo para incorporar ideias novas**.
- Aceitar no máximo **três melhorias opcionais por rodada** (até seis ao todo), selecionadas pelo coordenador por benefício mensurável, risco baixo, facilidade de compreensão e ausência de custo recorrente. Correções de bugs, segurança, falhas graves de acessibilidade e regressões são **obrigatórias** e não consomem essa cota.
- Toda melhoria adotada deve ter **teste objetivo e resultado registrado**. Não alterar dosagens, prescrições, critérios de alerta sensíveis, permissões, tratamento de dados de saúde, escopo de vigilância ou fluxos de emergência sem validação adequada; tratar propostas de alto risco como pendentes de revisão humana, não como implantação automática.
- Não adicionar serviços pagos, anúncios, rastreamento invasivo, tecnologias não licenciadas ou integrações que exijam cartões de crédito sem aprovação. Não usar root ou ADB como dependência de usuários finais.
- **Parar o ciclo de melhoria** depois da Rodada B, correção dos defeitos críticos encontrados, execução dos testes acordados e registro dos itens futuros. Evitar refatorações intermináveis e 'melhorias' sem benefício demonstrado.

### Evidências e entregáveis da orquestração

Criar no repositório:

- `AGENTS.md` — instruções persistentes da colaboração, regras de arquivo, segurança, escopo e critérios de teste; **deve ser lido e seguido** quando o ambiente Codex reconhecer esse arquivo.
- `docs/AGENT_WORK_PLAN.md` — quadro de tarefas, agentes/áreas responsáveis, dependências, contratos de interfaces e estado de integração. Não inventar a execução de agentes indisponíveis.
- `docs/IMPROVEMENTS.md` — duas rodadas, achados dos revisores, priorização, decisão (implementada/adiada/descartada), justificativa, teste e evidência de aceitação.
- `IMPLEMENTATION_REPORT.md` — agentes efetivamente usados, atividades delegadas, testes executados, limitações reais do ambiente, recursos não verificados e o que ainda depende de configuração/teste em aparelhos.

**Aceite RF-15:** houve delegação real se suportada (ou fallback descrito sem falsas alegações); duas revisões documentadas; melhorias adotadas foram testadas; nenhum requisito importante foi descartado; nenhum dado clínico/credencial real foi compartilhado com revisores; o sistema continua utilizável offline nas funções essenciais.

## 1. Objetivo e públicos

Criar aplicativo **gratuito para o usuário**, **offline-first**, acessível a idosos e pessoas com dificuldade de memória ou baixa familiaridade com smartphones, com apoio de cuidadores/familiares autorizados. Evitar anúncios, assinatura, rastreamento comercial e coleta excessiva. Interface sem menus complexos: botões grandes, alto contraste, linguagem curta, leitura em voz alta e contatos por foto + texto. **Internacionalização desde a primeira versão**: estrutura preparada para várias línguas, com interface inicial em português-BR e recursos/localizações para português-PT, inglês, japonês e espanhol, sem depender de tradução automática online para executar lembretes. Dar suporte a **dois modos** no mesmo APK: **Pessoa Assistida** e **Familiar/Cuidador**. A pessoa assistida deve conseguir operar tarefas essenciais sem precisar cadastrar dados ou configurar permissões sozinha.

**Não é dispositivo médico, não diagnostica, não altera prescrições nem garante vigilância/resgate.** Oferecer interfaces e estados explícitos para indisponibilidade de rede, GPS, permissões, aparelho desligado ou bateria descarregada. Em possível desorientação, **orientar o familiar a encontrar a pessoa, nunca induzir o idoso a caminhar desacompanhado por instruções de navegação da IA**.

## 2. Arquitetura técnica mandatória

- **Localização/i18n obrigatória**: manter todas as mensagens da interface, notificações, alarmes, acessibilidade e falas em recursos de idioma (sem strings de interface hardcoded no Kotlin). Usar `Locale`/recursos Android por idioma, seleção por aplicativo (Android 13+ e AndroidX compatível em anteriores), fallback explícito e testes de recursos ausentes. A API deve transportar códigos de idioma BCP 47 e os avisos devem ser localizados por **destinatário**, não apenas pela língua do perfil da pessoa assistida.
- **App Android nativo**: Kotlin, Jetpack Compose e Material 3, arquitetura modular organizada (camadas de apresentação, domínio, dados), Room para base local, DataStore para preferências, coroutines/Flow, WorkManager para trabalhos tolerantes a atrasos, AlarmManager para lembretes nos horários precisos após avaliação de permissões aplicáveis, BroadcastReceiver para reinicialização/estado de bateria, Android TextToSpeech, CameraX/Photo Picker e ML Kit Text Recognition **bundled** para OCR offline. GeofencingClient/serviços de localização com suporte transparente a atrasos e permissões. Preferir minSdk compatível com bibliotecas estáveis e Android 13–16 como alvo prioritário; targetSdk vigente com comportamento testado. Registrar fuso horário de cada perfil e fazer cálculos de recorrência respeitando horário de verão e alterações de fuso. Considere testes nos aparelhos com otimização agressiva de bateria, sem prometer execução exata em todos.
- **Autenticação para familiares**, sem exigir conta remota para o uso local: Firebase Authentication com e-mail/senha ou outra opção sem custo de autenticação telefônica; autenticar API verificando tokens com bibliotecas adequadas. Nada de senha/segredo hardcoded no APK. Vínculo de familiares por convite de uso único e expiração; acesso separado por pessoa assistida e papel de familiar; revogação imediata.
- **Backend econômico**: Cloudflare Workers + D1 (plano Free para piloto, com limites), API HTTPS autenticada, Workers Cron Triggers para eventos temporizados, eventos idempotentes e controle de duplicatas. Integrar Firebase Cloud Messaging (FCM) via HTTP v1 **exclusivamente pelo backend**, autenticado e autorizado; credencial de serviço **nunca no app** ou no repositório. Enviar apenas aos tokens dos familiares realmente autorizados, identificados no servidor, e lidar com tokens expirados. Manter tratamento de rate limits/retries e registrar últimos estados conhecidos. Evitar Firebase Cloud Functions que exijam ativação de plano pago para implantação. Verificar e documentar quotas, preços e necessidade de políticas de uso. O FCM é gratuito, mas requer conectividade e não garante imediatismo.
- **Offline-first e sincronização**: dados básicos e alarmes operam localmente; sincronizar alterações oportunisticamente; resolver conflitos por versão e origem, com prioridade à preservação de alterações verificadas por humano. O backend precisa de uma cópia **mínima e consentida** das programações necessárias para lembretes familiares em horários sincronizados, inclusive quando o aparelho do idoso ficar sem rede. Registrar evento de confirmação com carimbo temporal; deduplicar e distinguir status 'sem confirmação' de 'não tomou'.
- **Privacidade**: proteção de armazenamento local usando recursos Android Keystore + criptografia adequada para arquivos clínicos; HTTPS; segregação de dados por usuário no backend e autorização no nível de cada solicitação; logs sem conteúdo médico, localização precisa ou tokens; exportação e exclusão de dados; consentimento específico para dados de saúde e localização; privilégios mínimos e política de retenção. Áudios e imagens de consulta permanecem **locais por padrão**, sem upload automático.
- **Integração IA local ou opcional**: lógica determinística para alarmes, contatos, geofencing e regras de segurança. Adotar interfaces pluggable `TranscriptionEngine`, `DocumentExtractionEngine` e `ConsultationSummaryEngine`; usar mecanismos gratuitos/offline quando compatíveis e entregar fallback manual plenamente funcional. Nunca inventar transcrições, prescrições ou 'confiança' não calculada. Se ASR/LLM local grande não rodar no dispositivo-alvo, preservar captura, revisão e edição manual em vez de depender de chave paga. IA em nuvem futura somente por opt-in explícito com aviso de processamento de informações de saúde.

## 3. Requisitos funcionais integrados

### RF-01 — Início acessível
Tela inicial da pessoa assistida com as opções equivalentes localizadas para 'Meu dia', 'Medicamentos', 'Consultas', 'Minha família', 'Preciso de ajuda', 'Gravar consulta'. Botões grandes, voiceover por TTS com voz local configurada; fonte escalável; suporte TalkBack; contraste e áreas de toque confortáveis. Perfil de demonstração usando **nomes fictícios**. Lembretes falados no idioma escolhido para o perfil assistido, incluindo português-BR; fornecer opção de repetição e controles de volume pertinentes. Se voz offline apropriada estiver ausente, manter aviso visual e sinal sonoro; explicar como instalar a voz com ajuda do familiar.

### RF-02 — Medicamentos e confirmações
Cadastro pelo familiar: nome, apresentação, dosagem **conforme prescrição**, horário, dias, data de início/fim, observação, fonte conferida, status de validação. Alertar por voz com modelo de frase **localizado** (exemplo pt-BR: 'São oito horas. Está na hora do medicamento [nome], [dosagem conforme cadastro].'). Preservar literalmente nomes e doses cadastrados; não traduzir automaticamente prescrições. Botões **Já tomei**, **Ainda não tomei**, **Lembrar depois**. Registrar confirmação com horário real, possibilidade de correção auditável; ausência de resposta **nunca equivale a dose omitida**. Não orientar dose extra, dobrada ou alteração de esquema.

### RF-03 — Agenda e exames
Consultas e exames, horários, local, preparos prescritos, documentos a levar, alertas configuráveis; tela diária offline e sincronização para familiar autorizado quando disponível.

### RF-04 — Contatos familiares por foto E identificação
Cada contato contém fotografia local/opcional, primeiro nome OU apelido OU identificação como 'Filha', 'Neto', 'Tia', campo opcional de vínculo, telefone e forma de contato. A exibição deve mostrar foto + identificação configurável, não apenas imagem. Botões grandes 'Ligar' (intent discador) e 'WhatsApp' (quando instalado); respeitar permissões, não exigir acesso integral à agenda do Android.

### RF-05 — OCR de prescrições
Foto do documento → OCR local com ML Kit → campos de medicamento/consulta **sugeridos** e mostrados em tela de revisão. Indicar texto original visível e marcações de dúvida. **Nunca ativar medicamento nem alterar dose automaticamente**; familiar ou pessoa autorizada confirma manualmente. Se dados estiverem ilegíveis, apresentar 'não foi possível reconhecer com segurança'.

### RF-06 — Memória da consulta
Gravação iniciada/parada conscientemente na tela visível, após indicar necessidade de consentimento de participantes; microfone nunca é acionado ocultamente. Salvar áudio local protegido. Transcrição local opcional se motor/modelo disponível; fallback manual e opção de importar transcrição. Organizar propostas de 'medicamentos', 'exames', 'retorno', 'orientações' com evidência ou referência ao trecho, marcadas como **rascunho não validado**. Um familiar autorizado revisa/edita e só então publica lembretes. Não supor datas inexistentes.

### RF-07 — Rede de Apoio Familiar: parâmetros DEFINIDOS
- No horário de medicamento, notificar **todos os familiares autorizados** que ativaram esse tipo de aviso.
- Se **15 minutos após** o horário não houver confirmação registrada/sincronizada, enviar aviso adicional a **todos os familiares autorizados** (com preferência editável por pessoa).
- Texto: 'Sem confirmação registrada', **não** 'não tomou'.
- Se chegou confirmação antes da segunda notificação, cancelar a segunda; se uma confirmação ocorreu offline e ainda não foi sincronizada, o aviso adicional deverá indicar possibilidade de atraso na sincronização e permitir atualização posterior sem falsear o histórico.
- Autorizar familiar a configurar os avisos, desligar avisos de medicamentos específicos e revogar seu acesso. Push FCM preferencial, sem divulgar medicamentos na tela bloqueada por padrão; dados detalhados somente após desbloqueio e autorização.
- Criar agendador backend com minutos precisos e tolerâncias documentadas; idempotência para impedir mensagens duplicadas, retries seguros e monitoramento de erros.

### RF-08 — Ajuste de identidade no contato
A foto NÃO substitui a identificação textual. Permitir 'João', 'Meu filho', 'Filha Maria' etc. Os nomes exibidos são definidos por família/pessoa assistida, sem obrigatoriedade de nome completo.

### RF-09 — Volta Segura (opt-in)
- Área segura definida com ajuda do familiar e consentimento da pessoa assistida; raio editável e visualização clara. Detectar transição de saída por geofencing conforme compatibilidade e restrições do SO; **avisar todos os familiares autorizados assim que a saída for detectada** (não prometer detecção instantânea).
- Em paralelo, voz local pergunta: 'Olá, [nome], está tudo bem? Precisa de ajuda?'. 'Sem resposta' ou resposta confusa é apenas **possível sinal de risco**, jamais diagnóstico. Iniciar diálogo por microfone apenas se permitido e com fluxo de interação que respeite restrições Android de gravação em segundo plano; se não puder capturar áudio, usar notificação de alta visibilidade e botões grandes.
- Botão 'Preciso de ajuda' exibirá contato familiar principal e opção 'Estou com alguém que pode ajudar'. Com iniciativa/consentimento da pessoa assistida, abrir tela para terceiro voluntário fornecer rua/referência; comparar com coordenadas e precisão atuais, sinalizando compatibilidade, divergência ou impossibilidade de validação. **Não divulgar diagnósticos ou prontuário a terceiros**.
- Família recebe mapa/link da última localização disponível com **timestamp e precisão**, opção de abrir mapa para o **familiar ir até o assistido** e ligar. Nunca orientar pessoa potencialmente desorientada a atravessar ruas ou navegar sozinha por comandos de IA. Cartão de emergência offline com contatos. Sem vigilância oculta; desligar/remover acesso permitido.

### RF-10 — Proteção de bateria
Monitorar nível e carregamento via Android. Ao chegar a **30%**: notificação + fala local e aviso simultâneo aos familiares autorizados quando houver rede; em **15%**, aviso crítico com prevenção de spam. Não exigir resposta para confirmar carregamento; verificar sinal elétrico real de carregamento para registrar estado e avisar família. Mensagens atrasadas quando desconectado devem ter timestamp e não parecerem atuais.

### RF-11 — Conectividade
Detectar estados de acesso à internet e localização. Mostrar aviso simples quando dados/localização forem desativados; abrir ajustes oficiais com autorização. Não tentar reativar GPS/dados móveis em silêncio nem solicitar root/ADB como requisito. Recomendar Wi-Fi e dados móveis previamente ativados, permitindo alternância nativa. Monitorar heartbeat remoto de forma parcimoniosa; se deixar de chegar por intervalo **configurável** com margem para falhas de rede, enviar a familiares aviso neutro: 'Sem comunicação desde [horário]', sem inferir causa.

### RF-12 — Permissões e consentimento
Central de Permissões com justificativas legíveis e botões que conduzem às solicitações oficiais Android: `POST_NOTIFICATIONS`, localização aproximada/precisa e, apenas para Volta Segura, localização em segundo plano em etapa própria, `RECORD_AUDIO` para gravar consulta conscientemente, câmera/imagens com Photo Picker ou CameraX, alarmes exatos quando apropriado e concedidos. Pedir permissões em contexto, não todas de uma vez. Mostrar claramente quando permissões não estão disponíveis. Nenhuma permissão para ler SMS/chamadas. **Não incluir `SEND_SMS` no manifest da versão distribuível**: restrição do Google Play. Incluir opcional 'Compartilhar por SMS' por `ACTION_SENDTO`/`smsto:` que abre app de mensagens para **o usuário confirmar**; não tratar como SMS automático. Push automático via FCM e backend para familiares vinculados. Para contatos, usar dados informados dentro do LembreBem e discador explícito sem `CALL_PHONE` se possível.

### RF-13 — Auditoria e segurança
Trilha de alterações de horários/medicamentos com quem aprovou, data e versão; sem PHI em logs. Acesso granular por familiar; convites revogáveis. Modo de privacidade para notificações em tela bloqueada. Exportação, exclusão local e remota e retenção configurável de áudios. Estado de falha claramente visível. Não tratar o app como ferramenta de emergência garantida. Documentação LGPD e declaração de privacidade de rascunho para revisão jurídica antes de uso público.

## 4. Fluxos de cadastro e uso

**Primeira utilização:** escolher Modo Pessoa Assistida ou Familiar; app pode operar 100% local com dados fictícios e sem criar conta. Para receber avisos remotos, familiar cria conta não telefônica e associa por convite consentido; mostra testes de notificação sem expor informação médica.

**Medicamento real:** familiar cadastra → texto e dados são revisados → salva local e sincroniza horário mínimo autorizado → aviso sonoro/local na hora → push ao familiar → possível push adicional após 15 min sem confirmação. Lembrar de reconciliação quando o celular estiver offline.

**Saída da área segura:** evento detectado → backend recebe localização mínima/timestamp se há internet → todos familiares autorizados são avisados → celular pergunta de modo acessível se precisa de ajuda → familiar abre link de rota para chegar à pessoa → suporte opcional de terceiro, sem dados clínicos.

**Queda de conexão:** alertas locais funcionam independentemente da internet; heartbeat backend indica 'sem comunicação'; jamais envia dados inventados ou afirma localização em tempo real se apenas há posição antiga.

## 5. Interface e requisitos de qualidade

- Idioma padrão pt-BR e suporte internacionalizado a pt-PT, en-US, ja-JP, es-ES desde a primeira versão integrada, conforme seção de internacionalização abaixo. Linguagem respeitosa, fontes grandes, acessibilidade com TalkBack; fotos com descrição e nomes; confirmação de ações perigosas; suporte a dispositivo de entrada simples.
- Cores com contraste; interface limpa, sem banners e sem anúncios. O aplicativo não deve depender de leitura complexa.
- Não pedir que idoso se oriente sozinho em situação de risco; chamar familiar, manter pessoa em local seguro quando possível.
- Estabelecer indicadores de disponibilidade de conectividade, permissões concedidas e **qualidade da localização**; avisar se dado estiver desatualizado.

## 5A. Internacionalização e expansão global — requisito obrigatório (RF-14)

Implementar agora a **infraestrutura de localização**, e não deixá-la como melhoria futura. O Brasil será o piloto; a arquitetura deve permitir atender outras regiões **sem duplicar o aplicativo, o banco ou a lógica de alarmes**.

**Idiomas/variantes inicialmente planejados:**

- `pt-BR` — Português (Brasil), idioma padrão e piloto.
- `pt-PT` — Português (Portugal), tradução própria (não presumir que português europeu equivale ao brasileiro).
- `en-US` — English (United States), com estrutura preparada para `en-GB` posteriormente.
- `ja-JP` — 日本語 (Japão); suportar tipografia japonesa, texto mais longo/curto e acessibilidade adequada.
- `es-ES` — Español (España), com estrutura preparada para `es-419` (América Latina) posteriormente.

**Implementação mandatória:**

1. Externalizar todas as strings em recursos Android `res/values*/strings.xml`, incluindo plurais, leitores de tela, botões, consentimentos, mensagens de emergência, títulos e corpos de notificações push. Não usar strings de interface codificadas diretamente no código. Adicionar tradução inicial para os cinco idiomas previstos e documentação para revisão humana; textos de saúde e segurança não revisados **não podem ser declarados prontos para lançamento público**.
2. Incluir seletor acessível **Idioma / Language / Idioma / 日本語** no primeiro acesso e nas configurações de familiar, com opção 'Usar idioma do celular'. A família pode configurar o idioma da pessoa assistida. Integrar preferências de idioma por app do Android (Android 13+) e opção compatível em versões anteriores. Evitar que seleção de idioma elimine dados ou compromissos.
3. Aplicar formatação regional de datas, dias, horário 12h/24h conforme preferência/região, números, calendários, fusos e períodos. Armazenar instantes com UTC e fuso IANA quando necessário; nunca depender do idioma para calcular horários de medicamentos ou alarmes.
4. A voz deve ler mensagens no idioma da pessoa assistida usando `TextToSpeech` com `Locale` adequada e voz offline instalada quando disponível; se faltarem dados de voz, apresentar instruções acessíveis para baixar pacote, mantendo alertas visuais/sonoros. Reconhecimento de fala e transcrição devem indicar claramente idiomas **suportados no aparelho** e fallback manual; não prometer reconhecimento offline em todos os idiomas.
5. Permitir idioma **independente** para pessoa assistida e para cada cuidador. Por exemplo: lembrete local em japonês e push ao familiar em inglês. O backend deve montar textos de push a partir de modelos localizados e preferências do destinatário, com fallback explícito, sem expor dados clínicos em tela bloqueada.
6. Preservar **literalmente** nomes de medicamentos, dosagens, texto de receitas, transcrições e endereços registrados. Não converter prescrições nem interpretar unidades/doses por tradução automática. Tradução clínica, quando oferecida no futuro, requer opt-in e revisão qualificada.
7. Separar localização de interface de legislação e protocolos de serviço: números de emergência, telefones, serviços de saúde, tratamento de dados e textos legais variam por país; não assumir números brasileiros fora do Brasil. O país de uso deve ser uma configuração clara e distinta do idioma.
8. A IA e prompts de suporte por voz devem usar respostas no idioma configurado, com fallback apropriado. Registrar testes de layout, acessibilidade e notificações em todas as variantes prometidas. Não exigir APIs pagas de tradução nem conexão contínua para apresentar a interface traduzida.
9. Disponibilizar instruções no README para acrescentar novos idiomas sem alterar as regras de negócio, incluindo `en-GB`, `es-419` e outras variantes que forem validadas.

**Critério de aceite RF-14:** escolher japonês muda interface, alertas e conteúdo textual autorizado sem alterar horários; dois cuidadores de idiomas diferentes recebem notificações no próprio idioma; trocar idioma não apaga dados; ausência de pacote de fala offline não silencia alarmes; testes detectam chaves de tradução ausentes. Antes da distribuição internacional, requer revisão humana das traduções de saúde/segurança e revisão jurídica local.

## 6. Esquema de dados e APIs

Modelar entidades locais e remotas necessárias: `AssistedPerson`, `Caregiver`, `ConsentGrant`, `ContactCard`, `MedicationPlan`, `MedicationDoseEvent`, `DoseAcknowledgement`, `Appointment`, `MedicalDocumentLocal`, `ConsultationRecordingLocal`, `ConsultationDraft`, `GeofenceConfig`, `LocationEvent`, `BatteryEvent`, `ConnectivityEvent`, `PushSubscription`, `AuditEvent`, `DeviceHeartbeat`. Incluir `preferredLocale` BCP 47 para perfil assistido e individualmente para cada familiar (p. ex. `pt-BR`, `ja-JP`) e `countryCode` separado para regras regionais. Usar UUIDs, UTC no transporte e exibição no fuso do usuário. Documentar migrações, índices e TTLs. Não expor tokens FCM nem serviço Firebase no app; API recusa qualquer requisição de familiar não autorizado. Não colocar dados sensíveis na carga push por padrão; push aponta para detalhe autenticado.

Rotas essenciais: cadastrar/gerenciar vínculos consentidos; atualizar programação validada; receber estado/confirmações de doses; registrar tokens FCM; sincronizar eventos; eventos de geofence, bateria e conexão; obter avisos do familiar; revogar acessos; excluir/exportar dados. Documentar OpenAPI ou equivalente e validação de cada rota.

## 7. Testes automatizados e critérios de aceite

Crie testes unitários, instrumentados quando cabíveis e cenários simulados para:

1. Medicamento 08:00 dispara aviso local sem internet; 'Já tomei' grava data e hora; relógio/fuso corretos; após reiniciar celular, os alarmes são reprogramados.
2. Familiares A e B autorizados recebem push no horário quando conectados; 15 min depois recebem aviso se não há confirmação; confirmação antecipada cancela aviso; sem confirmação não é classificado como dose perdida.
3. Confirmação feita offline, sincronizada depois: evitar estados contraditórios, sinalizar atraso e registrar carimbo real.
4. Só familiar autorizado pode ver alertas, localizações e dados compartilhados; usuário removido perde acesso. Testar IDOR, autenticação ausente, tokens vencidos, replay, rate limits.
5. Bateria cruza 30% e 15% com deduplicação; conectar carregador gera alteração de estado verificada (sem apenas confiar em toque 'Já carreguei').
6. Saída da área segura aciona evento/aviso, respeitando atraso do geofence; falta de GPS/rede gera estados transparentes; localização antiga apresenta timestamp; rota abre no dispositivo do **cuidador**.
7. Camera/OCR propõe campos sem cadastrar medicamento automaticamente; gravação não começa sem interação e indicação de consentimento; transcrição e propostas nunca viram prescrição ativa sem revisão.
8. Permissões negadas não quebram o app: fallback explícito. Sem rede, fluxo offline funcional; sem FCM configurado, modo demo **rotulado**.
9. UI acessível: botões e texto suficientes, labels TalkBack, teste de alto contraste, fonte grande, navegação simples.
10. Teste de integração em **dois Androids** (ou dispositivo + emulador): pessoa assistida e familiar; backend local simulado e, com credenciais autorizadas, ambiente real.
11. **Internacionalização**: seleção do idioma por perfil/dispositivo; strings completas e sem chaves faltando em pt-BR, pt-PT, en-US, ja-JP e es-ES; envio de push em idioma do familiar distinto do idioma do idoso; textos de lembretes e avisos de risco localizados; formatos de data, hora e fuso; fontes ampliadas e quebra de linha japonesa; fallback quando TTS offline ou reconhecimento de voz não suportar uma língua; ausência de tradução automática de nomes/doses de medicamentos.

**Critério de conclusão:** projeto compila; testes executados e resultado relatado; banco com migrações; APK de testes gerado **se ambiente Android SDK permitir**; execução offline demonstrável; backend configurável sem segredos hardcoded; README com configuração Firebase/Cloudflare, permissões e limites gratuitos, comandos build/test, instruções de instalar APK no Android, avaliação LGPD e riscos. Não declarar pronto para uso clínico ou emergencial sem testes de campo e revisão adequada.

## 8. Entregáveis concretos em repositório único

```text
lembrebem/
  README.md
  AGENTS.md                   # regras de colaboração e revisão do Codex
  IMPLEMENTATION_REPORT.md    # delegação real, testes e limitações
  ARCHITECTURE.md
  SECURITY_PRIVACY.md
  TEST_PLAN.md
  android/                   # app Gradle Android Kotlin/Compose
  backend/                   # Cloudflare Workers + D1 + migrations + cron
  docs/                      # telas, manual, AGENT_WORK_PLAN.md e IMPROVEMENTS.md
  scripts/                   # comandos locais de teste e build
  .env.example               # nomes de variáveis, nenhum segredo
  .gitignore
```

Se não tiver Android SDK no ambiente do Codex, implemente o código por completo, rode validações disponíveis e **informe precisamente o que não foi testado**, sem inventar um APK ou afirmar sucesso de compilação. Todos os serviços externos devem ter modo de desenvolvimento simulado bem sinalizado; configure as integrações reais com chaves providas apenas pelo proprietário no console, nunca copiadas em mensagens públicas.

## 9. Referências oficiais a verificar

- Firebase FCM e precificação: https://firebase.google.com/docs/cloud-messaging ; https://firebase.google.com/pricing
- Android internacionalização e idiomas por aplicativo: https://developer.android.com/guide/topics/resources/app-languages
- Android compatibilidade cultural e idiomas: https://developer.android.com/training/basics/supporting-devices/languages
- Android permissões de notificações: https://developer.android.com/develop/ui/compose/notifications/notification-permission
- Políticas SMS: https://support.google.com/googleplay/android-developer/answer/16558241
- Intents SMS com confirmação: https://developer.android.com/guide/components/intents-common
- AlarmManager: https://developer.android.com/develop/background-work/services/alarms
- Restrições em segundo plano: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- ML Kit OCR local: https://developers.google.com/ml-kit/vision/text-recognition/v2/android
- Cloudflare Workers Free: https://developers.cloudflare.com/workers/platform/limits/
- D1: https://developers.cloudflare.com/d1/platform/pricing/
- Cron: https://developers.cloudflare.com/workers/configuration/cron-triggers/
- Boas práticas multiagentes: https://developers.openai.com/api/docs/guides/agents-api/multi-agent
- Iteração baseada em validação: https://developers.openai.com/cookbook/examples/codex/build_iterative_repair_loops_with_codex
- AGENTS.md e fluxo Codex: https://developers.openai.com/cookbook/examples/codex/iterating-development-workflows-with-codex

## 10. Instrução final de execução

**Comece a criar os arquivos agora e orquestre subagentes conforme RF-15 quando essa capacidade estiver disponível.** Faça decisões técnicas razoáveis com base nas exigências e documente-as; não devolva apenas planejamento. Entregue o conjunto integrado e executável com os recursos descritos, distinguindo claramente funcionalidades implementadas, as que requerem credenciais externas e as que dependem de avaliação/testes físicos. No encerramento, reporte arquitetura, arquivos criados, comandos executados, testes realizados, falhas conhecidas, cobertura de requisitos **incluindo RF-14/i18n, RF-15/delegação e melhorias, e idiomas realmente implementados** e passos mínimos para o proprietário instalar e testar com **dados fictícios**.
