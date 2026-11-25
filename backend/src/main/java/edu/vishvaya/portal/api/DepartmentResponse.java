package edu.vishvaya.portal.api;

import java.io.Serializable;

public record DepartmentResponse(String code, String name) implements Serializable {
}
