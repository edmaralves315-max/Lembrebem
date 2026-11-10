# Segurança e privacidade — rascunho para revisão jurídica/LGPD

Finalidade: apoio consentido a lembretes e familiares; não diagnóstico, prescrição nem garantia de resgate. Não há anúncios, análise comercial ou serviços pagos obrigatórios. Dados de saúde e localização têm consentimentos separados. O proprietário deve definir controlador, canal de direitos, base legal adequada, operadores, transferências e resposta a incidentes antes de uso público.

O uso local não exige conta. Familiares remotos usam Firebase email/senha; API valida assinatura, audience, issuer e validade. Convites de uso único expiram24h; autorização e revogação por pessoa/escopo. Tokens/conta de serviço não aparecem em logs nem exemplos. Credencial FCM privada somente como segredo Worker; configuração pública Firebase fornecida pelo proprietário no app. HTTPS obrigatório no app. Não incluir SEND_SMS/CALL_PHONE; intents exigem ação do usuário.

Áudios/imagens finais cifrados localmente; Room usa sandbox Android e criptografia do dispositivo, sem criptografia adicional do banco por coluna. Bloqueio do aparelho é necessário. Áudio temporário durante uso não é cifrado. Retenção de áudio7/30/90 dias aplicada ao abrir módulo, não garantia de expurgo contínuo em app inativo. Não há upload automático de anexos. Exportação local é ação explícita e produz JSON legível: destino deve ser protegido. Exportação não inclui todos os binários/rascunhos clínicos. Exclusões local/remota são separadas e confirmadas.

Push é genérico, detalhes via API autenticada; localização antiga conserva timestamp/precisão. Geofence opt-in pode atrasar. Pessoa desorientada recebe apoio para contatar familiar, nunca rota para caminhar sozinha. Ausência de resposta não prova dose omitida. Famílias devem planejar apoio fora do aplicativo para falhas de bateria/rede/serviço.

Não foi feita auditoria de penetração, revisão jurídica ou clínica. Traduções ainda sem validação qualificada. Leia relatórios A/B para controles testados e pendências. Políticas e quotas oficiais precisam ser reconfirmadas antes do piloto.
