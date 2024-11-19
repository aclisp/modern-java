package com.mycompany.web.beans;

public record JdbcHandlerReq(
        String emailLike) {

    public void validate() {
        if (emailLike == null)
            throw new IllegalArgumentException("emailLike不能为空");
    }
}
