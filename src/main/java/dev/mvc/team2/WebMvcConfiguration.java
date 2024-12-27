package dev.mvc.team2;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import dev.mvc.board.Board;
import dev.mvc.grade.Grade;
import dev.mvc.member.Member;
import dev.mvc.survey.Survey;


@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer{
  
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    
      // C:/kd/deploy/team2/board/storage ->  /board/storage -> http://localhost:9093/board/storage;
      registry.addResourceHandler("/board/storage/**").addResourceLocations("file:///" +  Board.getUploadDir());
      
      // C:/kd/deploy/team2/survey/storage ->  /survey/storage -> http://localhost:9093/survey/storage;
      registry.addResourceHandler("/survey/storage/**").addResourceLocations("file:///" +  Survey.getUploadDir());
      
      System.out.println("Upload Directory: " + Board.getUploadDir());
      
      // C:/kd/deploy/team2/member/storage ->  /board/storage -> http://localhost:9093/board/storage;
      registry.addResourceHandler("/member/storage/**").addResourceLocations("file:///" +  Member.getUploadDir());
      
      // C:/kd/deploy/team2/grade/storage ->  /grade/storage -> http://localhost:9093/grade/storage;
      registry.addResourceHandler("/grade/storage/**").addResourceLocations("file:///" +  Grade.getUploadDir());

  }

}
