package com.smarttool.logger.dto;

import lombok.Data;

@Data
public class ExplainResponse {
    private String explanation;
    private String rootCause;
    private String fix;
}
