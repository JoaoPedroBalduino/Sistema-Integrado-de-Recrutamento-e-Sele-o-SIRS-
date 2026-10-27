package br.com.sirs.api.model.enums;

public enum StatusVaga {
  RASCUNHO,
  EM_APROVACAO,
  PUBLICADA,
  CONGELADA,
  FECHADA;

  public boolean permits(StatusVaga proximoStatus) {
    return switch (this) {
      case RASCUNHO -> proximoStatus == EM_APROVACAO;
      case EM_APROVACAO -> proximoStatus == PUBLICADA || proximoStatus == RASCUNHO;
      case PUBLICADA -> proximoStatus == CONGELADA || proximoStatus == FECHADA;
      case CONGELADA -> proximoStatus == PUBLICADA || proximoStatus == FECHADA;
      case FECHADA -> false;
    };
  }
}
