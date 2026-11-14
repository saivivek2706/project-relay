package com.azentio.projectrelay.web;
import jakarta.validation.constraints.NotNull;
public record StatusPatchRequest(@NotNull String status, String note, String actor){}
