package dev.mvc.diary;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dev.mvc.member.MemberProcInter;
import dev.mvc.emotion.EmotionVO;
import dev.mvc.tool.Tool;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/diary")
public class DiaryCont {
  @Autowired
  @Qualifier("dev.mvc.diary.DiaryProc")
  private DiaryProcInter diaryProc;

  @Autowired
  @Qualifier("dev.mvc.member.MemberProc") 
  private MemberProcInter memberProc;
  
  /** 페이지당 출력할 레코드 갯수, nowPage는 1부터 시작 */
  public int record_per_page = 10;

  /** 블럭당 페이지 수, 하나의 블럭은 10개의 페이지로 구성됨 */
  public int page_per_block = 10;

  /** 페이징 목록 주소 */
  private String list_file_name = "/diary/list_search";

  public DiaryCont() {
    System.out.println("-> CateCont created.");
  }

  /**
   * 등록 폼
   * 
   * @param model
   * @return
   */
  // http://localhost:9091/diary/create/ X
  // http://localhost:9091/diary/create
  @GetMapping(value = "/create")
  public String create(Model model, 
                                  @RequestParam(name="title", defaultValue="오늘의 제목") String title, 
                                  @RequestParam(name="emotion", defaultValue="0") int emotion, 
                                  @RequestParam(name="summary", defaultValue="오늘의 일기") String summary) {
    // create method에 사용될 테이블
    // summary를 가져올 테이블
    DiaryVO diaryVO = new DiaryVO();
    EmotionVO emotionVO = new EmotionVO();
    
    model.addAttribute("diaryVO", diaryVO);
    model.addAttribute("emotionVO", emotionVO);

    diaryVO.setTitle(title);
    diaryVO.setSummary(summary);
    diaryVO.setEmno(emotion);
    
    return "/diary/create"; // /templates/diary/create.html
  }

  /**
   * 등록 처리, http://localhost:9091/diary/create
   * 
   * @param model         Controller -> Thymeleaf HTML로 데이터 전송에 사용
   * @param diaryVO        Form 태그 값 -> 검증 -> diaryVO 자동 저장, request.getParameter()
   *                      자동 실행
   * @param bindingResult 폼에 에러가 있는지 검사 지원
   * @return
   */
  @PostMapping(value = "/create")
  public String create(Model model, 
                                  @Valid @ModelAttribute("diaryVO") DiaryVO diaryVO, 
                                  @ModelAttribute("emotionVO") EmotionVO emotionVO, 
                                  //감정이나 코드 테이블의 VO 삽입할 곳
                                  //@ModelAttribute("VO") VO VO, 
                                  BindingResult bindingResult) {
    if (bindingResult.hasErrors() == true) { 
      // 에러가 있으면 폼으로 돌아갈 것.
      // model.addAttribute("diaryVO", diaryVO);
      return "/diary/create"; // /templates/diary/create.html
    }

    diaryVO.setTitle(diaryVO.getTitle().trim());
    diaryVO.setEmno(diaryVO.getEmno());
    diaryVO.setSummary(diaryVO.getSummary().trim());
    
    int cnt = this.diaryProc.create(diaryVO);
    System.out.println("-> create_cnt: " + cnt);

    if (cnt == 1) {
      // model.addAttribute("code", "create_success");
      // model.addAttribute("name", diaryVO.getName());

      // return "redirect:/diary/list_all"; // @GetMapping(value="/list_all")
      return "redirect:/diary/list_search"; // @GetMapping(value="/list_all")
    } else {
      model.addAttribute("code", "create_fail");
    }

    model.addAttribute("cnt", cnt);

    return "/diary/msg"; // /templates/diary/msg.html
  }

  /**
   * 등록 폼 및 목록
   * 
   * @param model
   * @return
   */
  // http://localhost:9091/diary/list_all
  @GetMapping(value = "/list_all")
  public String list_all(Model model) {
    DiaryVO diaryVO = new DiaryVO();
    
    // requestParam으로 불러와야함.
    diaryVO.setIllustno(0);
    diaryVO.setEmno(0);
    diaryVO.setWeather_code(0);
    model.addAttribute("diaryVO", diaryVO);

    ArrayList<DiaryVO> list = this.diaryProc.list_all();
    model.addAttribute("list", list);

    return "/diary/list_all"; // /templates/diary/list_all.html
  }

  /**
   * 조회 
   * http://localhost:9091/diary/read/1
   */
  @GetMapping(value = "/read/{diaryno}")
  public String read(Model model, 
                     @PathVariable("diaryno") Integer diaryno, 
                     @RequestParam(name = "title", defaultValue = "") String title, 
                     @RequestParam(name = "date", defaultValue = "") String date, 
                     @RequestParam(name = "sort", defaultValue = "DESC") String sort, 
                     @RequestParam(name = "now_page", defaultValue = "1") int now_page) {

      DiaryVO diaryVO = this.diaryProc.read(diaryno);
      model.addAttribute("diaryVO", diaryVO);

      // 검색 및 정렬 수행
      ArrayList<DiaryVO> list = this.diaryProc.list_search_paging(title.trim(), date.trim(), sort, now_page, this.record_per_page);
      model.addAttribute("list", list);

      model.addAttribute("title", title);
      model.addAttribute("date", date);
      model.addAttribute("sort", sort);

      // 페이징 처리
      int search_count = this.diaryProc.list_search_count(title.trim(), date.trim());
      String paging = this.diaryProc.pagingBox(now_page, title, date, this.list_file_name, search_count, this.record_per_page, this.page_per_block);
      model.addAttribute("paging", paging);
      model.addAttribute("now_page", now_page);
      

      // 일련 변호 생성: 레코드 갯수 - ((현재 페이지수 -1) * 페이지당 레코드 수)
      int no = search_count - ((now_page - 1) * this.record_per_page);
      model.addAttribute("no", no);
      // --------------------------------------------------------------------------------------

      return "/diary/read";
  }


  

  /**
   * 수정폼 http://localhost:9091/diary/update/1
   */
  @GetMapping(value = "/update/{diaryno}")
  public String update(HttpSession session, Model model, 
                                    @PathVariable("diaryno") Integer diaryno, 
                                    @RequestParam(name = "title", defaultValue = "") String title, 
                                    @RequestParam(name = "date", defaultValue = "") String date, 
                                    @RequestParam(name = "sort", defaultValue = "DESC") String sort, 
                                    @RequestParam(name = "now_page", defaultValue = "1") int now_page) {
    if (this.memberProc.isMemberAdmin(session)) {
      DiaryVO diaryVO = this.diaryProc.read(diaryno);
      model.addAttribute("diaryVO", diaryVO);

      // ArrayList<DiaryVO> list = this.diaryProc.list_all();
      ArrayList<DiaryVO> list = this.diaryProc.list_search_paging(title, date, sort, now_page, this.record_per_page);
      model.addAttribute("list", list);
      model.addAttribute("title", title);
      model.addAttribute("date", date);
      model.addAttribute("sort", sort);


      // 카테고리 그룹 목록
      ArrayList<String> list_genre = this.diaryProc.genreset();
      model.addAttribute("list_genre", String.join("/", list_genre));


      // --------------------------------------------------------------------------------------
      // 페이지 번호 목록 생성
      // --------------------------------------------------------------------------------------
      int search_count = this.diaryProc.list_search_count(title, date);
      String paging = this.diaryProc.pagingBox(now_page, title, date, this.list_file_name, search_count, this.record_per_page,
          this.page_per_block);
      model.addAttribute("paging", paging);
      model.addAttribute("now_page", now_page);

      // 일련 변호 생성: 레코드 갯수 - ((현재 페이지수 -1) * 페이지당 레코드 수)
      int no = search_count - ((now_page - 1) * this.record_per_page);
      model.addAttribute("no", no);
      // --------------------------------------------------------------------------------------

      return "/diary/update"; // templaes/diary/update.html
    } else {
      return "redirect:/member/login_cookie_need"; // redirect
    }
  }

  /**
   * 수정 처리, http://localhost:9091/diary/update
   * 
   * @param model         Controller -> Thymeleaf HTML로 데이터 전송에 사용
   * @param diaryVO        Form 태그 값 -> 검증 -> diaryVO 자동 저장, request.getParameter()
   *                      자동 실행
   * @param bindingResult 폼에 에러가 있는지 검사 지원
   * @return
   */
  @PostMapping(value = "/update")
  public String update(HttpSession session, Model model, 
                                    @Valid @ModelAttribute("diaryVO") DiaryVO diaryVO, BindingResult bindingResult,
                                    @RequestParam(name = "title", defaultValue = "") String title, 
                                    @RequestParam(name = "date", defaultValue = "") String date, 
                                    @RequestParam(name = "sort", defaultValue = "DESC") String sort, 
                                    @RequestParam(name = "now_page", defaultValue = "1") int now_page, RedirectAttributes ra) {
    if (this.memberProc.isMemberAdmin(session)) {
//    System.out.println("-> update post.");
    if (bindingResult.hasErrors() == true) { // 에러가 있으면 폼으로 돌아갈 것.
//      System.out.println("-> ERROR 발생");
      return "/diary/update"; // /templates/diary/update.html
    }

//    System.out.println(diaryVO.getName());
//    System.out.println(diaryVO.getSeqno());
//    System.out.println(diaryVO.getVisible());

    int cnt = this.diaryProc.update(diaryVO);
    System.out.println("-> cnt: " + cnt);

    if (cnt == 1) {
      ra.addAttribute("now_page", now_page); // redirect로 데이터 전송

      return "redirect:/diary/update/" + diaryVO.getDiaryno(); // @GetMapping(value="/update/{diaryno}")
    } else {
      model.addAttribute("code", "update_fail");
    }

    model.addAttribute("cnt", cnt);

    // --------------------------------------------------------------------------------------
    // 페이지 번호 목록 생성
    // --------------------------------------------------------------------------------------
    int search_count = this.diaryProc.list_search_count(title, date);
    String paging = this.diaryProc.pagingBox(now_page, title, date, this.list_file_name, search_count, this.record_per_page, this.page_per_block);
    model.addAttribute("paging", paging);
    model.addAttribute("now_page", now_page);

    // 일련 변호 생성: 레코드 갯수 - ((현재 페이지수 -1) * 페이지당 레코드 수)
    int no = search_count - ((now_page - 1) * this.record_per_page);
    model.addAttribute("no", no);
    // --------------------------------------------------------------------------------------

    return "/diary/msg"; // /templates/diary/msg.html
    } else {
      return "redirect:/member/login_cookie_need";  // redirect
    }
    
  }

  /**
   * 삭제폼 http://localhost:9091/diary/delete/1
   */
  @GetMapping(value = "/delete/{diaryno}")
  public String delete(HttpSession session, Model model, 
                                   @PathVariable("diaryno") Integer diaryno,
                                   @RequestParam(name = "title", defaultValue = "") String title, 
                                   @RequestParam(name = "date", defaultValue = "") String date, 
                                   @RequestParam(name = "sort", defaultValue = "DESC") String sort, 
                                   @RequestParam(name = "now_page", defaultValue = "1") int now_page) {
    if (this.memberProc.isMemberAdmin(session)) {
      DiaryVO diaryVO = this.diaryProc.read(diaryno);
      model.addAttribute("diaryVO", diaryVO);
      int cnt = this.diaryProc.cntcount(diaryno);

      // ArrayList<DiaryVO> list = this.diaryProc.list_all();
      ArrayList<DiaryVO> list = this.diaryProc.list_search_paging(title, date, sort, now_page, this.record_per_page);
      model.addAttribute("list", list);
      model.addAttribute("title", title);
      model.addAttribute("date", date);
      model.addAttribute("sort", sort);

      model.addAttribute("cnt", cnt);  // 콘텐츠 개수 추가
      model.addAttribute("now_page", now_page);

      // --------------------------------------------------------------------------------------
      // 페이지 번호 목록 생성
      // --------------------------------------------------------------------------------------
      int search_count = this.diaryProc.list_search_count(title, date);
      String paging = this.diaryProc.pagingBox(now_page, title, date, this.list_file_name, search_count, this.record_per_page,
          this.page_per_block);
      model.addAttribute("paging", paging);
      model.addAttribute("now_page", now_page);

      // 일련 변호 생성: 레코드 갯수 - ((현재 페이지수 -1) * 페이지당 레코드 수)
      int no = search_count - ((now_page - 1) * this.record_per_page);
      model.addAttribute("no", no);
      // --------------------------------------------------------------------------------------
      if (cnt == 0) {
        // 콘텐츠가 없을 경우 diary/delete.html로 이동
        return "/diary/delete";
      } else {
        model.addAttribute("cnt", cnt);
        model.addAttribute("title", title);
        model.addAttribute("date", date);
        model.addAttribute("now_page", now_page);
        return "/diary/list_all_delete"; // diary/list_all_delete.html로 이동
      }
    } else {
      return "redirect:/member/login_cookie_need"; // 관리자 권한 필요
    }
  }
  
  /**
   * 카테고리 및 연관 자료 삭제 처리
   */
  @PostMapping(value = "/delete_all_confirm")
  public String deleteAllCategory(@RequestParam (name="diaryno", defaultValue="0") int diaryno,
                                                       RedirectAttributes redirectAttributes) {

    // 카테고리 삭제
    diaryProc.delete(diaryno);

    redirectAttributes.addFlashAttribute("msg", "카테고리와 관련된 모든 자료가 삭제되었습니다.");
    return "redirect:/diary/list_search";
  }

  /**
   * 카테고리 삭제 폼
   */
  @GetMapping(value = "/delete")
  public String delete(Model model) {
    // 기본 삭제 폼
    return "/diary/delete";  // diary/delete.html로 이동
  }

  /**
   * 삭제 처리, http://localhost:9091/diary/delete?diaryno=1
   * 
   * @param model         Controller -> Thymeleaf HTML로 데이터 전송에 사용
   * @param diaryVO        Form 태그 값 -> 검증 -> diaryVO 자동 저장, request.getParameter()
   *                      자동 실행
   * @param bindingResult 폼에 에러가 있는지 검사 지원
   * @return
   */
  @PostMapping(value = "/delete")
  public String delete_process(HttpSession session, Model model, 
                               @RequestParam(name = "diaryno", defaultValue = "0") Integer diaryno,
                               @RequestParam(name = "title", defaultValue = "") String title, 
                               @RequestParam(name = "date", defaultValue = "") String date, 
                               @RequestParam(name = "sort", defaultValue = "DESC") String sort, 
                               @RequestParam(name = "now_page", defaultValue = "1") int now_page, 
                               RedirectAttributes ra) {
      // 관리자 권한 확인
      if (this.memberProc.isMemberAdmin(session)) {
          System.out.println("-> delete_process");

          DiaryVO diaryVO = this.diaryProc.read(diaryno); // 삭제 전에 레코드 조회
          model.addAttribute("diaryVO", diaryVO);

          // 카테고리에 속한 콘텐츠 개수 확인
          int cnt = this.diaryProc.cntcount(diaryno); // 해당 카테고리 내 콘텐츠 수

          if (cnt == 0) {
              // 콘텐츠가 없으면 카테고리만 삭제
              int deleteCnt = this.diaryProc.delete(diaryno);
              System.out.println("-> deleteCnt: " + deleteCnt);

              if (deleteCnt == 1) {
                  ra.addAttribute("title", title); // redirect로 데이터 전송
                  ra.addAttribute("date", date); // redirect로 데이터 전송

                  // 마지막 페이지에서 모든 레코드가 삭제되면 페이지수를 1 감소 시켜야 함.
                  int search_cnt = this.diaryProc.list_search_count(title, date);
                  if (search_cnt % this.record_per_page == 0) {
                      now_page = now_page - 1;
                      if (now_page < 1) {
                          now_page = 1; // 최소 시작 페이지
                      }
                  }

                  ra.addAttribute("now_page", now_page); // redirect로 데이터 전송

                  return "redirect:/diary/list_search"; // 카테고리 목록 페이지로 리다이렉트
              } else {
                  model.addAttribute("code", "delete_fail");
                  return "/diary/msg"; // 실패 메시지 출력
              }
          } else {
              model.addAttribute("cnt", cnt);
              model.addAttribute("title", title); 
              model.addAttribute("date", date);
              model.addAttribute("sort", sort);
              model.addAttribute("now_page", now_page);
              return "/diary/list_all_delete"; // diary/list_all_delete.html로 이동
          }
      } else {
          return "redirect:/member/login_cookie_need";  // 권한 없을 때 로그인 페이지로 리다이렉트
      }
  }


  /**
   * 우선 순위 높임, 10 등 -> 1 등, http://localhost:9091/diary/update_seqno_forward/1
   * 
   * @param model Controller -> Thymeleaf HTML로 데이터 전송에 사용
   * @return
   */
  @GetMapping(value = "/update_seqno_forward/{diaryno}")
  public String update_seqno_forward(Model model, @PathVariable("diaryno") Integer diaryno,
      @RequestParam(name = "word", defaultValue = "") String word,
      @RequestParam(name = "now_page", defaultValue = "") int now_page, RedirectAttributes ra) {
    this.diaryProc.update_seqno_forward(diaryno);

    ra.addAttribute("word", word); // redirect로 데이터 전송
    ra.addAttribute("now_page", now_page); // redirect로 데이터 전송

    return "redirect:/diary/list_search"; // @GetMapping(value="/list_search")
  }

  /**
   * 우선 순위 낮춤, 1 등 -> 10 등, http://localhost:9091/diary/update_seqno_backward/1
   * 
   * @param model Controller -> Thymeleaf HTML로 데이터 전송에 사용
   * @return
   */
  @GetMapping(value = "/update_seqno_backward/{diaryno}")
  public String update_seqno_backward(Model model, @PathVariable("diaryno") Integer diaryno,
      @RequestParam(name = "word", defaultValue = "") String word,
      @RequestParam(name = "now_page", defaultValue = "") int now_page, RedirectAttributes ra) {
    this.diaryProc.update_seqno_backward(diaryno);

    ra.addAttribute("word", word); // redirect로 데이터 전송
    ra.addAttribute("now_page", now_page); // redirect로 데이터 전송

    return "redirect:/diary/list_search"; // @GetMapping(value="/list_search")
  }

  /**
   * 카테고리 공개 설정, http://localhost:9091/diary/update_visible_y/1
   * 
   * @param model Controller -> Thymeleaf HTML로 데이터 전송에 사용
   * @return
   */
  @GetMapping(value = "/update_visible_y/{diaryno}")
  public String update_visible_y(HttpSession session, Model model, @PathVariable("diaryno") Integer diaryno,
      @RequestParam(name = "word", defaultValue = "") String word,
      @RequestParam(name = "now_page", defaultValue = "") int now_page, RedirectAttributes ra) {
    
    if (this.memberProc.isMemberAdmin(session)) {
      this.diaryProc.update_visible_y(diaryno);

      ra.addAttribute("word", word); // redirect로 데이터 전송
      ra.addAttribute("now_page", now_page); // redirect로 데이터 전송

      return "redirect:/diary/list_search"; // @GetMapping(value="/list_search")
    } else {
      return "redirect:/member/login_cookie_need";  // redirect
    }
  }

  /**
   * 카테고리 비공개 설정, http://localhost:9091/diary/update_visible_n/1
   * 
   * @param model Controller -> Thymeleaf HTML로 데이터 전송에 사용
   * @return
   */
  @GetMapping(value = "/update_visible_n/{diaryno}")
  public String update_visible_n(HttpSession session, Model model, @PathVariable("diaryno") Integer diaryno,
      @RequestParam(name = "word", defaultValue = "") String word,
      @RequestParam(name = "now_page", defaultValue = "") int now_page, RedirectAttributes ra) {
    
    if (this.memberProc.isMemberAdmin(session)) {
      this.diaryProc.update_visible_n(diaryno);

      ra.addAttribute("word", word); // redirect로 데이터 전송
      ra.addAttribute("now_page", now_page); // redirect로 데이터 전송

      return "redirect:/diary/list_search"; // @GetMapping(value="/list_search")
    } else {
      return "redirect:/member/login_cookie_need";  // redirect
    }
  }

// 아래의 기능은 팀원들과의 상의가 필요함. 우리 앱의 일기 파트에서 유의미하게 검색 기능이 사용되는 순간이 없을 것 같다고 생각함.
//  /**
//   * 등록 폼 및 검색 목록 + 페이징 http://localhost:9091/diary/list_search
//   * http://localhost:9091/diary/list_search?word=&now_page=
//   * http://localhost:9091/diary/list_search?word=까페&now_page=1
//   * 
//   * @param model
//   * @return
//   */
//  @GetMapping(value = "/list_search")
//  public String list_search_paging(HttpSession session, Model model,
//                                                      @RequestParam(name = "word", defaultValue = "") String word,
//                                                      @RequestParam(name = "now_page", defaultValue = "1") int now_page) {
//    if (this.memberProc.isMemberAdmin(session)) {
//      DiaryVO diaryVO = new DiaryVO();
//      // diaryVO.setGenre("분류");
//      // diaryVO.setName("카테고리 이름을 입력하세요."); // Form으로 초기값을 전달
//
//      // 카테고리 그룹 목록
//      ArrayList<String> list_genre = this.diaryProc.genreset();
//      diaryVO.setTitle(String.join("/", list_genre));
//
//      model.addAttribute("diaryVO", diaryVO);
//
//      word = Tool.checkNull(word);
//
//      ArrayList<DiaryVO> list = this.diaryProc.list_search_paging(word, now_page, this.record_per_page);
//      model.addAttribute("list", list);
//
////      ArrayList<DiaryVO> menu = this.diaryProc.list_all_diarygrp_y();
////      model.addAttribute("menu", menu);
//      
//      for (DiaryVO diary : list) {
//        if (diary.getName().equals("--")) {  // 대분류인 경우
//            int totalCnt = 0;
//
//            // 중분류 카테고리에서 대분류에 속하는 자료 수를 합산
//            for (DiaryVO subCate : list) {
//                if (!subCate.getName().equals("--") && subCate.getGenre().equals(diary.getGenre())) {
//                    totalCnt += this.diaryProc.cntcount(subCate.getCateno());
//                    }
//                } 
//            diary.setCnt(totalCnt); // 대분류 카테고리의 자료 수 설정
//        } else {
//            int contentsCount = this.diaryProc.cntcount(diary.getCateno()); // 각 중분류 카테고리의 자료 수 조회
//            diary.setCnt(contentsCount); // DiaryVO 객체에 자료 수 설정
//        }
//      }
//
//
//      int search_cnt = this.diaryProc.list_search_count(word);
//      model.addAttribute("search_cnt", search_cnt);
//
//      model.addAttribute("word", word); // 검색어
//
//      // --------------------------------------------------------------------------------------
//      // 페이지 번호 목록 생성
//      // --------------------------------------------------------------------------------------
//      int search_count = this.diaryProc.list_search_count(word);
//      String paging = this.diaryProc.pagingBox(now_page, word, this.list_file_name, search_count, this.record_per_page,
//          this.page_per_block);
//      model.addAttribute("paging", paging);
//      model.addAttribute("now_page", now_page);
//
//      // 일련 변호 생성: 레코드 갯수 - ((현재 페이지수 -1) * 페이지당 레코드 수)
//      int no = search_count - ((now_page - 1) * this.record_per_page);
//      model.addAttribute("no", no);
//      // --------------------------------------------------------------------------------------
//
//      return "/diary/list_search"; // /templates/diary/list_search.html
//    } else {
//      return "redirect:/member/login_cookie_need"; // redirect
//    }
//
//  }

}
