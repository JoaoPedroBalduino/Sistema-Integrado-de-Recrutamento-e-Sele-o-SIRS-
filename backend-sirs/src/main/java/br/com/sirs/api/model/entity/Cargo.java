package br.com.sirs.api.model.entity;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cargo")
@NoArgsConstructor
public class Cargo extends CadastroApoio {}
