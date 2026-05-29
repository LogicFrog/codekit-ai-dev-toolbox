package org.itfjnu.codekit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("需要完整运行环境（MySQL/Redis），在 CI 中单独运行")
class CodekitApplicationTests {

    @Test
    void contextLoads() {
    }

}
