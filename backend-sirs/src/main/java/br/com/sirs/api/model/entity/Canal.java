package br.com.sirs.api.model.entity;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "canal")
@NoArgsConstructor
public class Canal extends CadastroApoio {}
