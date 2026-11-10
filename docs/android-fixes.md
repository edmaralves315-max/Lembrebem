# Correções Android — revisão A, 10/11/2026

Responsável real: subagente `/root/android_fixes`. Código anterior preservado; alterações limitadas a Android não clínico e este relatório.

- S-A1: política explícita `Rules.maySync` separa saúde, localização e eventos independentes; heartbeat, bateria, ajuda e FCM não dependem do consentimento de saúde. Dados sem consentimento permanecem locais; um cuidador não envia telemetria de seu aparelho para o perfil assistido.
- S-A4: geofence sem posição envia `locationUnavailable:true`, omite coordenadas e não inventa precisão. Pull ignora ausência de posição. HTTP 400/403/409/422 gera auditoria `sync_rejected_STATUS` sem payload e libera eventos seguintes; fila retém a linha original, marcada processada. Nova edição/revisão gera novo evento. HTTP408/429/5xx solicita retry. Falha401 interrompe para preservar autenticação. Status de falha é visível em Avisos familiares.
- S-A5: próxima ocorrência é agendada antes de decidir se a dose atual já foi tomada. Broadcast repetido não elimina recorrência futura.
- UA1: ajuda é botão fixo anterior à área rolável, em todos os fluxos não clínicos após escolha de modo. Um toque abre ajuda; ligação continua exigindo seleção explícita do contato.
- UA2: notificação privada inclui apenas ID interno no PendingIntent explícito, abre a dose correspondente, inclusive onNewIntent/reabertura repetida. Conteúdo público permanece genérico. Identidade PendingIntent inclui URI da dose para evitar colisão entre hashes.
- UA3: falha de inicialização, voz offline ausente e erro de reprodução geram bipe e preferência persistente; tela inicial mostra orientação e instalação de voz. Voz e chamadas TTS são confinadas ao main looper. Manifest inclui visibilidade de TTS_SERVICE.
- Inspeção adicional encontrou erro Kotlin anterior em `Field`: lambda estava antes de parâmetro Boolean final, incompatível com trailing lambda usado nas chamadas. Corrigida assinatura e chamada de senha.
- Exclusão local agora limpa também configurações públicas Firebase e estado TTS.

## Evidência disponível

Executado Python/XML: cinco idiomas com 168 chaves iguais; todos os XML Android parseiam; todas as referências Kotlin R.string resolvem. Esses resultados não substituem compilação.

Adicionados quatro testes JVM reais em RulesTest: consentimentos independentes, isolamento de telemetria cuidador, política retry HTTP e recorrência após dose tomada. Adicionados dois testes instrumentados Compose: ajuda visível 360×640dp/fonte2 e abertura da dose específica. Não executados neste subagente: não havia SDK, Gradle nem kotlinc instalado no PATH. Coordenador registra eventual execução posterior.

Permanecem testes físicos: som real e TTS ERROR, bloqueio de tela, PendingIntent cold start, alarmes após boot, geofence real, fonte2/TalkBack e sincronização em dois aparelhos. UA4 (seletores amigáveis) fica adiada por decisão do coordenador. UA5 pertence ao responsável clínico.
