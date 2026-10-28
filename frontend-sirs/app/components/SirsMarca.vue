<script setup lang="ts">
withDefaults(defineProps<{ variante?: 'horizontal' | 'simbolo' }>(), { variante: 'horizontal' })
</script>

<template>
  <span class="sirs-marca" :class="{ 'sirs-marca-simbolo': variante === 'simbolo' }">
    <svg
      v-if="variante === 'horizontal'"
      viewBox="235 290 810 253"
      role="img"
      aria-label="SIRS · Recrutamento e seleção"
      focusable="false"
    >
      <image href="/branding/logo-sirs-horizontal.png" width="1280" height="832" />
    </svg>
    <svg v-else viewBox="274 239 476 545" role="img" aria-label="Símbolo SIRS" focusable="false">
      <image href="/branding/simbolo-sirs.png" width="1024" height="1024" />
    </svg>
  </span>
</template>

<style scoped>
.sirs-marca {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 0;
  border-radius: 0;
  background: #fff;
  flex-shrink: 0;
}
.sirs-marca svg {
  display: block;
  width: 100%;
  height: auto;
  aspect-ratio: 810 / 253;
  overflow: hidden;
}
.sirs-marca-simbolo {
  width: 64px;
  height: 64px;
  padding: 6px;
}
.sirs-marca-simbolo svg {
  aspect-ratio: 476 / 545;
  max-height: 100%;
}
</style>
