# Arquitetura

Android: Kotlin/Compose Material3, camadas ui/domain/data/platform/clinical. Room v1 armazena perfil, planos, doses, agenda, contatos, auditoria, outbox, geofence e última localização. DataStore mantém preferências. AlarmManager/TTS são locais; WorkManager sincroniza oportunisticamente. Aplicativo suporta um perfil local por instalação. Modo familiar local não é autenticação; proteja o aparelho.

Backend: Cloudflare Worker autenticado por Firebase JWT RS256, D1 com migração `0001_initial.sql`, cron por minuto e FCM HTTP v1 somente no servidor. Testes e demonstração usam SQLite real do Node e mesma API/scheduler. Demo identidade fictícia só em servidor loopback; Worker recusa bypass demo.

Contratos: UUIDs, instantes UTC ISO8601, fuso IANA, idioma BCP47 e país separado. Fonte/dose literais e revisão humana. Outbox idempotente, versões de plano e auditoria; rejeições permanentes não bloqueiam outros eventos e ficam auditadas, exigindo revisão para reenvio. Eventos de saída sem posição usam locationUnavailable, sem coordenadas inventadas. Sync usa cursor recebido/id para paginação determinística; Android confirma cursor após aplicar página e limita dez páginas por execução, retomando por retry.

Clinical: ML Kit bundled Latin/Japanese por idioma do app; Photo Picker; MediaRecorder apenas após interação consentida; arquivos finais AES-GCM/Keystore em noBackupFilesDir. Transcrição automática não fornecida: digitação/importação manual funcional. Resumo organiza trechos literais em rascunhos sem acesso para ativar prescrições. Temporários de áudio em uso ficam privados, sem cifra, apagados ao finalizar/reabrir módulo.

Room inicia em versão1, exportSchema habilitado via KSP; schema ainda não gerado sem compilação. Não existe migração de versão Android anterior. Futuras mudanças exigem Migration e teste, nunca fallback destrutivo. D1: índices/TTLs na migração e cron; eventos/alertas90 dias, planos/auditoria até exclusão. Não há anexos clínicos remotos.
