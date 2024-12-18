package dev.mvc.reply;

import java.util.ArrayList;
import java.util.HashMap;

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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dev.mvc.board.BoardProcInter;
import dev.mvc.board.BoardVO;
import dev.mvc.member.MemberProcInter;
import dev.mvc.tool.Tool;
import dev.mvc.tool.Upload;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RequestMapping(value = "/reply")
@Controller
public class ReplyCont {
  @Autowired
  @Qualifier("dev.mvc.member.MemberProc") // @Service("dev.mvc.member.MemberProc")
  private MemberProcInter memberProc;

  @Autowired
  @Qualifier("dev.mvc.board.BoardProc") // @Component("dev.mvc.board.BoardProc")
  private BoardProcInter boardProc;

  @Autowired
  @Qualifier("dev.mvc.reply.ReplyProc") // @Component("dev.mvc.contents.ContentsProc")
  private ReplyProcInter replyProc;

  public ReplyCont() {
    System.out.println("-> ReplyCont created.");
  }

  /**
   * POST 요청시 새로고침 방지, POST 요청 처리 완료 → redirect → url → GET → forward -> html 데이터
   * 전송
   * 
   * @return
   */
  @GetMapping(value = "/post2get")
  public String post2get(Model model, 
      @RequestParam(name="url", defaultValue="") String url) {
    ArrayList<BoardVOMenu> menu = this.boardProc.menu();
    model.addAttribute("menu", menu);

    return url; // forward, /templates/...
  }

  // 등록 폼, contents 테이블은 FK로 cateno를 사용함.
  // http://localhost:9093/contents/create X
  // http://localhost:9093/contents/create?cateno=1 // cateno 변수값을 보내는 목적
  // http://localhost:9093/contents/create?cateno=2
  // http://localhost:9093/contents/create?cateno=5
  @GetMapping(value = "/create")
  public String create(Model model, 
      @ModelAttribute("replyVO") ReplyVO replyVO, 
      @RequestParam(name="boardno", defaultValue="0") int boardno) {
    ArrayList<BoardVOMenu> menu = this.boardProc.menu();
    model.addAttribute("menu", menu);

    BoardVO boardVO = this.boardProc.read(boardno); // 카테고리 정보를 출력하기위한 목적
    model.addAttribute("boardVO", boardVO);

    return "/reply/create"; // /templates/reply/create.html
  }

  /**
   * 등록 처리 http://localhost:9093/reply/create
   * 
   * @return
   */
  @PostMapping(value = "/create")
  public String create(HttpServletRequest request, 
      HttpSession session, 
      Model model, 
      @ModelAttribute("replyVO") ReplyVO replyVO,
      RedirectAttributes ra) {
 // Call By Reference: 메모리 공유, Hashcode 전달
    int memberno = (int) session.getAttribute("memberno"); // memberno FK
    replyVO.setMemberno(memberno);
    int cnt = this.replyProc.create(replyVO);

    // ------------------------------------------------------------------------------
    // PK의 return
    // ------------------------------------------------------------------------------
    // System.out.println("--> contentsno: " + contentsVO.getContentsno());
    // mav.addObject("contentsno", contentsVO.getContentsno()); // redirect
    // parameter 적용
    // ------------------------------------------------------------------------------

    if (cnt == 1) {
      // type 1, 재업로드 발생
      // return "<h1>파일 업로드 성공</h1>"; // 연속 파일 업로드 발생

      // type 2, 재업로드 발생
      // model.addAttribute("cnt", cnt);
      // model.addAttribute("code", "create_success");
      // return "contents/msg";

      // type 3 권장
      // return "redirect:/contents/list_all"; // /templates/contents/list_all.html

      // System.out.println("-> contentsVO.getCateno(): " + contentsVO.getCateno());
      // ra.addFlashAttribute("cateno", contentsVO.getCateno()); // controller ->
      // controller: X

      ra.addAttribute("replyno", replyVO.getReplyno()); // controller -> controller: O
      return "redirect:/reply/list_by_replyno";

      // return "redirect:/contents/list_by_cateno?cateno=" + contentsVO.getCateno();
      // // /templates/contents/list_by_cateno.html
    } else {
      ra.addFlashAttribute("code", "create_fail"); // DBMS 등록 실패
      ra.addFlashAttribute("cnt", 0); // 업로드 실패
      ra.addFlashAttribute("url", "/reply/msg"); // msg.html, redirect parameter 적용
      return "redirect:/reply/msg"; // Post -> Get - param...
    }
  } 
    

  /**
   * 전체 목록, 관리자만 사용 가능 http://localhost:9093/contents/list_all
   * 
   * @return
   */
  @GetMapping(value = "/list_all")
  public String list_all(HttpSession session, Model model) {
    // System.out.println("-> list_all");
    ArrayList<CateVOMenu> menu = this.cateProc.menu();
    model.addAttribute("menu", menu);

    if (this.memberProc.isMemberAdmin(session)) { // 관리자만 조회 가능
      ArrayList<ContentsVO> list = this.contentsProc.list_all(); // 모든 목록

      // Thymeleaf는 CSRF(크로스사이트) 스크립팅 해킹 방지 자동 지원
      // for문을 사용하여 객체를 추출, Call By Reference 기반의 원본 객체 값 변경
//      for (ContentsVO contentsVO : list) {
//        String title = contentsVO.getTitle();
//        String content = contentsVO.getContent();
//        
//        title = Tool.convertChar(title);  // 특수 문자 처리
//        content = Tool.convertChar(content); 
//        
//        contentsVO.setTitle(title);
//        contentsVO.setContent(content);  
//
//      }

      model.addAttribute("list", list);
      return "/contents/list_all";

    } else {
      return "redirect:/member/login_cookie_need";

    }

  }

//  /**
//   * 유형 1
//   * 카테고리별 목록
//   * http://localhost:9093/contents/list_by_cateno?cateno=5
//   * http://localhost:9093/contents/list_by_cateno?cateno=6 
//   * @return
//   */
//  @GetMapping(value="/list_by_cateno")
//  public String list_by_cateno(HttpSession session, Model model, 
//      @RequestParam(name="cateno", defaultValue = "") int cateno) {
//    ArrayList<CateVOMenu> menu = this.cateProc.menu();
//    model.addAttribute("menu", menu);
//    
//     CateVO cateVO = this.cateProc.read(cateno);
//     model.addAttribute("cateVO", cateVO);
//    
//    ArrayList<ContentsVO> list = this.contentsProc.list_by_cateno(cateno);
//    model.addAttribute("list", list);
//    
//    // System.out.println("-> size: " + list.size());
//
//    return "/contents/list_by_cateno";
//  }

//  /**
//   * 유형 2
//   * 카테고리별 목록 + 검색
//   * http://localhost:9093/contents/list_by_cateno?cateno=5
//   * http://localhost:9093/contents/list_by_cateno?cateno=6 
//   * @return
//   */
//  @GetMapping(value="/list_by_cateno")
//  public String list_by_cateno_search(HttpSession session, Model model, 
//                                                   @RequestParam(name="cateno", defaultValue = "0" ) int cateno, 
//                                                   @RequestParam(name="word", defaultValue = "") String word) {
//    ArrayList<CateVOMenu> menu = this.cateProc.menu();
//    model.addAttribute("menu", menu);
//    
//     CateVO cateVO = this.cateProc.read(cateno);
//     model.addAttribute("cateVO", cateVO);
//    
//     word = Tool.checkNull(word).trim(); // 검색어 공백 삭제
//     
//     HashMap<String, Object> map = new HashMap<>();
//     map.put("cateno", cateno);
//     map.put("word", word);
//     
//    ArrayList<ContentsVO> list = this.contentsProc.list_by_cateno_search(map);
//    model.addAttribute("list", list);
//    
//    // System.out.println("-> size: " + list.size());
//    model.addAttribute("word", word);
//    
//    int search_count = this.contentsProc.list_by_cateno_search_count(map);
//    model.addAttribute("search_count", search_count);
//    
//    return "/contents/list_by_cateno_search"; // /templates/contents/list_by_cateno_search.html
//  }

  /**
   * 유형 3
   * 카테고리별 목록 + 검색 + 페이징 http://localhost:9093/contents/list_by_cateno?cateno=5
   * http://localhost:9093/contents/list_by_cateno?cateno=6
   * 
   * @return
   */
  @GetMapping(value = "/list_by_replyno")
  public String list_by_replyno_search_paging(
      HttpSession session, 
      Model model, 
      @RequestParam(name = "replyno", defaultValue = "1") int replyno,
      @RequestParam(name = "word", defaultValue = "") String word,
      @RequestParam(name = "now_page", defaultValue = "1") int now_page) {

    // System.out.println("-> replyno: " + replyno);

    ArrayList<BoardVOMenu> menu = this.boardProc.menu();
    model.addAttribute("menu", menu);

    BoardVO boardVO = this.boardProc.read(boardno);
    model.addAttribute("boardVO", boardVO);

    word = Tool.checkNull(word).trim();

    HashMap<String, Object> map = new HashMap<>();
    map.put("boardno", boardno);
    map.put("word", word);
    map.put("now_page", now_page);

    ArrayList<ReplyVO> list = this.replyProc.list_by_boardno_search_paging(map);
    model.addAttribute("list", list);

    // System.out.println("-> size: " + list.size());
    model.addAttribute("word", word);

    int search_count = this.contentsProc.list_by_cateno_search_count(map);
    String paging = this.contentsProc.pagingBox(cateno, now_page, word, "/contents/list_by_cateno", search_count,
        Contents.RECORD_PER_PAGE, Contents.PAGE_PER_BLOCK);
    model.addAttribute("paging", paging);
    model.addAttribute("now_page", now_page);

    model.addAttribute("search_count", search_count);

    // 일련 변호 생성: 레코드 갯수 - ((현재 페이지수 -1) * 페이지당 레코드 수)
    int no = search_count - ((now_page - 1) * Contents.RECORD_PER_PAGE);
    model.addAttribute("no", no);

    return "/contents/list_by_cateno_search_paging"; // /templates/contents/list_by_cateno_search_paging.html
  }

  /**
   * 카테고리별 목록 + 검색 + 페이징 + Grid
   * http://localhost:9093/contents/list_by_cateno?cateno=5
   * http://localhost:9093/contents/list_by_cateno?cateno=6
   * 
   * @return
   */
  @GetMapping(value = "/list_by_cateno_grid")
  public String list_by_cateno_search_paging_grid(HttpSession session, 
      Model model, 
      @RequestParam(name = "cateno", defaultValue = "0") int cateno,
      @RequestParam(name = "word", defaultValue = "") String word,
      @RequestParam(name = "now_page", defaultValue = "1") int now_page) {

    // System.out.println("-> cateno: " + cateno);

    ArrayList<CateVOMenu> menu = this.cateProc.menu();
    model.addAttribute("menu", menu);

    CateVO cateVO = this.cateProc.read(cateno);
    model.addAttribute("cateVO", cateVO);

    word = Tool.checkNull(word).trim();

    HashMap<String, Object> map = new HashMap<>();
    map.put("cateno", cateno);
    map.put("word", word);
    map.put("now_page", now_page);

    ArrayList<ContentsVO> list = this.contentsProc.list_by_cateno_search_paging(map);
    model.addAttribute("list", list);

    // System.out.println("-> size: " + list.size());
    model.addAttribute("word", word);

    int search_count = this.contentsProc.list_by_cateno_search_count(map);
    String paging = this.contentsProc.pagingBox(cateno, now_page, word, "/contents/list_by_cateno_grid", search_count,
        Contents.RECORD_PER_PAGE, Contents.PAGE_PER_BLOCK);
    model.addAttribute("paging", paging);
    model.addAttribute("now_page", now_page);

    model.addAttribute("search_count", search_count);

    // 일련 변호 생성: 레코드 갯수 - ((현재 페이지수 -1) * 페이지당 레코드 수)
    int no = search_count - ((now_page - 1) * Contents.RECORD_PER_PAGE);
    model.addAttribute("no", no);

    // /templates/contents/list_by_cateno_search_paging_grid.html
    return "/contents/list_by_cateno_search_paging_grid";
  }

  /**
   * 조회 http://localhost:9093/contents/read?contentsno=17
   * 
   * @return
   */
  @GetMapping(value = "/read")
  public String read(Model model, 
      @RequestParam(name="contentsno", defaultValue = "0") int contentsno, 
      @RequestParam(name="word", defaultValue = "") String word, 
      @RequestParam(name="now_page", defaultValue = "1") int now_page) {
    
    ArrayList<CateVOMenu> menu = this.cateProc.menu();
    model.addAttribute("menu", menu);

    ContentsVO contentsVO = this.contentsProc.read(contentsno);

//    String title = contentsVO.getTitle();
//    String content = contentsVO.getContent();
//    
//    title = Tool.convertChar(title);  // 특수 문자 처리
//    content = Tool.convertChar(content); 
//    
//    contentsVO.setTitle(title);
//    contentsVO.setContent(content);  

    long size1 = contentsVO.getSize1();
    String size1_label = Tool.unit(size1);
    contentsVO.setSize1_label(size1_label);

    model.addAttribute("contentsVO", contentsVO);

    CateVO cateVO = this.cateProc.read(contentsVO.getCateno());
    model.addAttribute("cateVO", cateVO);

    // 조회에서 화면 하단에 출력
    // ArrayList<ReplyVO> reply_list = this.replyProc.list_contents(contentsno);
    // mav.addObject("reply_list", reply_list);

    model.addAttribute("word", word);
    model.addAttribute("now_page", now_page);

    return "/contents/read";
  }

  /**
   * 맵 등록/수정/삭제 폼 http://localhost:9093/contents/map?contentsno=19
   * 
   * @return
   */
  @GetMapping(value = "/map")
  public String map(Model model, 
                            @RequestParam(name="contentsno", defaultValue="0") int contentsno,
                            @RequestParam(name="word", defaultValue="") String word,
                            @RequestParam(name="now_page", defaultValue="1") int now_page) {
    ArrayList<CateVOMenu> menu = this.cateProc.menu();
    model.addAttribute("menu", menu);

    ContentsVO contentsVO = this.contentsProc.read(contentsno); // map 정보 읽어 오기
    model.addAttribute("contentsVO", contentsVO); // request.setAttribute("contentsVO", contentsVO);

    CateVO cateVO = this.cateProc.read(contentsVO.getCateno()); // 그룹 정보 읽기
    model.addAttribute("cateVO", cateVO);
    
    model.addAttribute("word", word);
    model.addAttribute("now_page", now_page);

    return "/contents/map"; // //templates/contents/map.html
  }

  /**
   * MAP 등록/수정/삭제 처리 http://localhost:9093/contents/map
   * 
   * @param contentsVO
   * @return
   */
  @PostMapping(value = "/map")
  public String map_update(Model model, 
      @RequestParam(name="contentsno", defaultValue = "0") int contentsno, 
      @RequestParam(name="map", defaultValue = "") String map) {
    HashMap<String, Object> hashMap = new HashMap<String, Object>();
    hashMap.put("contentsno", contentsno);
    hashMap.put("map", map);

    this.contentsProc.map(hashMap);

    return "redirect:/contents/read?contentsno=" + contentsno;
  }

  /**
   * Youtube 등록/수정/삭제 폼 http://localhost:9093/contents/youtube?contentsno=1
   * 
   * @return
   */
  @GetMapping(value = "/youtube")
  public String youtube(Model model, 
      @RequestParam(name="contentsno", defaultValue="0") int contentsno,
      @RequestParam(name="word", defaultValue="")  String word, 
      @RequestParam(name="now_page", defaultValue="0") int now_page) {
    ArrayList<CateVOMenu> menu = this.cateProc.menu();
    model.addAttribute("menu", menu);

    ContentsVO contentsVO = this.contentsProc.read(contentsno); // map 정보 읽어 오기
    model.addAttribute("contentsVO", contentsVO); // request.setAttribute("contentsVO", contentsVO);

    CateVO cateVO = this.cateProc.read(contentsVO.getCateno()); // 그룹 정보 읽기
    model.addAttribute("cateVO", cateVO);

    model.addAttribute("word", word);
    model.addAttribute("now_page", now_page);
    
    return "/contents/youtube";  // forward, /templates/contents/youtube.html
  }

  /**
   * Youtube 등록/수정/삭제 처리 http://localhost:9093/contents/youtube
   * 
   * @param contentsVO
   * @return
   */
  @PostMapping(value = "/youtube")
  public String youtube_update(Model model, 
                                            RedirectAttributes ra,
                                            @RequestParam(name="contentsno", defaultValue = "0") int contentsno, 
                                            @RequestParam(name="youtube", defaultValue = "") String youtube, 
                                            @RequestParam(name="word", defaultValue = "") String word, 
                                            @RequestParam(name="now_page", defaultValue = "0") int now_page) {

    if (youtube.trim().length() > 0) { // 삭제 중인지 확인, 삭제가 아니면 youtube 크기 변경
      youtube = Tool.youtubeResize(youtube, 640); // youtube 영상의 크기를 width 기준 640 px로 변경
    }

    HashMap<String, Object> hashMap = new HashMap<String, Object>();
    hashMap.put("contentsno", contentsno);
    hashMap.put("youtube", youtube);

    this.contentsProc.youtube(hashMap);
    
    ra.addAttribute("contentsno", contentsno);
    ra.addAttribute("word", word);
    ra.addAttribute("now_page", now_page);

    // return "redirect:/contents/read?contentsno=" + contentsno;
    return "redirect:/contents/read";
  }

  /**
   * 수정 폼 http:// localhost:9093/contents/update_text?contentsno=1
   *
   */
  @GetMapping(value = "/update_text")
  public String update_text(HttpSession session, 
      Model model, 
      @RequestParam(name="contentsno", defaultValue="") int contentsno, 
      RedirectAttributes ra, 
      @RequestParam(name="word", defaultValue="") String word,
      @RequestParam(name="now_page", defaultValue="1") int now_page) {
    ArrayList<CateVOMenu> menu = this.cateProc.menu();
    model.addAttribute("menu", menu);

    model.addAttribute("word", word);
    model.addAttribute("now_page", now_page);

    if (this.memberProc.isMemberAdmin(session)) { // 관리자로 로그인한경우
      ContentsVO contentsVO = this.contentsProc.read(contentsno);
      model.addAttribute("contentsVO", contentsVO);

      CateVO cateVO = this.cateProc.read(contentsVO.getCateno());
      model.addAttribute("cateVO", cateVO);

      return "/contents/update_text"; // /templates/contents/update_text.html
      // String content = "장소:\n인원:\n준비물:\n비용:\n기타:\n";
      // model.addAttribute("content", content);

    } else {
//      ra.addAttribute("url", "/member/login_cookie_need"); // /templates/member/login_cookie_need.html
//      return "redirect:/contents/msg"; // @GetMapping(value = "/read")
      return "member/login_cookie_need";
    }

  }

  /**
   * 수정 처리 http://localhost:9093/contents/update_text?contentsno=1
   * 
   * @return
   */
  @PostMapping(value = "/update_text")
  public String update_text(HttpSession session, 
      Model model, 
      @ModelAttribute("contentsVO") ContentsVO contentsVO, 
      RedirectAttributes ra,
      @RequestParam(name="search_word", defaultValue="") String search_word, // contentsVO.word와 구분 필요
      @RequestParam(name="now_page", defaultValue="0") int now_page) {
      ra.addAttribute("word", search_word);
      ra.addAttribute("now_page", now_page);

    if (this.memberProc.isMemberAdmin(session)) { // 관리자 로그인 확인
      HashMap<String, Object> map = new HashMap<String, Object>();
      map.put("contentsno", contentsVO.getContentsno());
      map.put("passwd", contentsVO.getPasswd());

      if (this.contentsProc.password_check(map) == 1) { // 패스워드 일치
        this.contentsProc.update_text(contentsVO); // 글수정

        // mav 객체 이용
        ra.addAttribute("contentsno", contentsVO.getContentsno());
        ra.addAttribute("cateno", contentsVO.getCateno());
        return "redirect:/contents/read"; // @GetMapping(value = "/read")

      } else { // 패스워드 불일치
        ra.addFlashAttribute("code", "passwd_fail"); // redirect -> forward -> html
        ra.addFlashAttribute("cnt", 0);
        ra.addAttribute("url", "/contents/msg"); // msg.html, redirect parameter 적용

        return "redirect:/contents/post2get"; // @GetMapping(value = "/msg")
      }
    } else { // 정상적인 로그인이 아닌 경우 로그인 유도
      ra.addAttribute("url", "/member/login_cookie_need"); // /templates/member/login_cookie_need.html
      return "redirect:/contents/post2get"; // @GetMapping(value = "/msg")
    }

  }

  /**
   * 파일 수정 폼 http://localhost:9093/contents/update_file?contentsno=1
   * 
   * @return
   */
  @GetMapping(value = "/update_file")
  public String update_file(HttpSession session, Model model, 
         @RequestParam(name="contentsno", defaultValue="0") int contentsno,
         @RequestParam(name="word", defaultValue="") String word, 
         @RequestParam(name="now_page", defaultValue="1") int now_page) {
    ArrayList<CateVOMenu> menu = this.cateProc.menu();
    model.addAttribute("menu", menu);
    
    model.addAttribute("word", word);
    model.addAttribute("now_page", now_page);
    
    ContentsVO contentsVO = this.contentsProc.read(contentsno);
    model.addAttribute("contentsVO", contentsVO);

    CateVO cateVO = this.cateProc.read(contentsVO.getCateno());
    model.addAttribute("cateVO", cateVO);

    return "/contents/update_file";

  }

  /**
   * 파일 수정 처리 http://localhost:9093/contents/update_file
   * 
   * @return
   */
  @PostMapping(value = "/update_file")
  public String update_file(HttpSession session, Model model, RedirectAttributes ra,
                            @ModelAttribute("contentsVO") ContentsVO contentsVO,
                            @RequestParam(name="word", defaultValue="") String word, 
                            @RequestParam(name="now_page", defaultValue="1") int now_page) {

    if (this.memberProc.isMemberAdmin(session)) {
      // 삭제할 파일 정보를 읽어옴, 기존에 등록된 레코드 저장용
      ContentsVO contentsVO_old = contentsProc.read(contentsVO.getContentsno());

      // -------------------------------------------------------------------
      // 파일 삭제 시작
      // -------------------------------------------------------------------
      String file1saved = contentsVO_old.getFile1saved(); // 실제 저장된 파일명
      String thumb1 = contentsVO_old.getThumb1(); // 실제 저장된 preview 이미지 파일명
      long size1 = 0;

      String upDir = Contents.getUploadDir(); // C:/kd/deploy/resort_v4sbm3c/contents/storage/

      Tool.deleteFile(upDir, file1saved); // 실제 저장된 파일삭제
      Tool.deleteFile(upDir, thumb1); // preview 이미지 삭제
      // -------------------------------------------------------------------
      // 파일 삭제 종료
      // -------------------------------------------------------------------

      // -------------------------------------------------------------------
      // 파일 전송 시작
      // -------------------------------------------------------------------
      String file1 = ""; // 원본 파일명 image

      // 전송 파일이 없어도 file1MF 객체가 생성됨.
      // <input type='file' class="form-control" name='file1MF' id='file1MF'
      // value='' placeholder="파일 선택">
      MultipartFile mf = contentsVO.getFile1MF();

      file1 = mf.getOriginalFilename(); // 원본 파일명
      size1 = mf.getSize(); // 파일 크기

      if (size1 > 0) { // 폼에서 새롭게 올리는 파일이 있는지 파일 크기로 체크 ★
        // 파일 저장 후 업로드된 파일명이 리턴됨, spring.jsp, spring_1.jpg...
        file1saved = Upload.saveFileSpring(mf, upDir);

        if (Tool.isImage(file1saved)) { // 이미지인지 검사
          // thumb 이미지 생성후 파일명 리턴됨, width: 250, height: 200
          thumb1 = Tool.preview(upDir, file1saved, 250, 200);
        }

      } else { // 파일이 삭제만 되고 새로 올리지 않는 경우
        file1 = "";
        file1saved = "";
        thumb1 = "";
        size1 = 0;
      }

      contentsVO.setFile1(file1);
      contentsVO.setFile1saved(file1saved);
      contentsVO.setThumb1(thumb1);
      contentsVO.setSize1(size1);
      // -------------------------------------------------------------------
      // 파일 전송 코드 종료
      // -------------------------------------------------------------------

      this.contentsProc.update_file(contentsVO); // Oracle 처리
      ra.addAttribute ("contentsno", contentsVO.getContentsno());
      ra.addAttribute("cateno", contentsVO.getCateno());
      ra.addAttribute("word", word);
      ra.addAttribute("now_page", now_page);
      
      return "redirect:/contents/read";
    } else {
      ra.addAttribute("url", "/member/login_cookie_need"); 
      return "redirect:/contents/post2get"; // GET
    }
  }

  /**
   * 파일 삭제 폼
   * http://localhost:9093/contents/delete?contentsno=1
   * 
   * @return
   */
  @GetMapping(value = "/delete")
  public String delete(HttpSession session, Model model, RedirectAttributes ra,
                               @RequestParam(name="cateno", defaultValue="") int cateno, 
                               @RequestParam(name="contentsno", defaultValue="0") int contentsno, 
                               @RequestParam(name="word", defaultValue="") String word, 
                               @RequestParam(name="now_page", defaultValue="1") int now_page) {
    if (this.memberProc.isMemberAdmin(session)) { // 관리자로 로그인한경우
      model.addAttribute("cateno", cateno);
      model.addAttribute("word", word);
      model.addAttribute("now_page", now_page);
      
      ArrayList<CateVOMenu> menu = this.cateProc.menu();
      model.addAttribute("menu", menu);
      
      ContentsVO contentsVO = this.contentsProc.read(contentsno);
      model.addAttribute("contentsVO", contentsVO);
      
      CateVO cateVO = this.cateProc.read(contentsVO.getCateno());
      model.addAttribute("cateVO", cateVO);
      
      return "/contents/delete"; // forward
      
    } else {
      ra.addAttribute("url", "/admin/login_cookie_need");
      return "redirect:/contents/msg"; 
    }

  }
  
  /**
   * 삭제 처리 http://localhost:9093/contents/delete
   * 
   * @return
   */
  @PostMapping(value = "/delete")
  public String delete(RedirectAttributes ra,
      @RequestParam(name="cateno", defaultValue="") int cateno, 
      @RequestParam(name="contentsno", defaultValue="0") int contentsno, 
      @RequestParam(name="word", defaultValue="") String word, 
      @RequestParam(name="now_page", defaultValue="1") int now_page) {
    // -------------------------------------------------------------------
    // 파일 삭제 시작
    // -------------------------------------------------------------------
    // 삭제할 파일 정보를 읽어옴.
    ContentsVO contentsVO_read = contentsProc.read(contentsno);
        
    String file1saved = contentsVO_read.getFile1saved();
    String thumb1 = contentsVO_read.getThumb1();
    
    String uploadDir = Contents.getUploadDir();
    Tool.deleteFile(uploadDir, file1saved);  // 실제 저장된 파일삭제
    Tool.deleteFile(uploadDir, thumb1);     // preview 이미지 삭제
    // -------------------------------------------------------------------
    // 파일 삭제 종료
    // -------------------------------------------------------------------
        
    this.contentsProc.delete(contentsno); // DBMS 삭제
        
    // -------------------------------------------------------------------------------------
    // 마지막 페이지의 마지막 레코드 삭제시의 페이지 번호 -1 처리
    // -------------------------------------------------------------------------------------    
    // 마지막 페이지의 마지막 10번째 레코드를 삭제후
    // 하나의 페이지가 3개의 레코드로 구성되는 경우 현재 9개의 레코드가 남아 있으면
    // 페이지수를 4 -> 3으로 감소 시켜야함, 마지막 페이지의 마지막 레코드 삭제시 나머지는 0 발생
    
    HashMap<String, Object> map = new HashMap<String, Object>();
    map.put("cateno", cateno);
    map.put("word", word);
    
    if (this.contentsProc.list_by_cateno_search_count(map) % Contents.RECORD_PER_PAGE == 0) {
      now_page = now_page - 1; // 삭제시 DBMS는 바로 적용되나 크롬은 새로고침등의 필요로 단계가 작동 해야함.
      if (now_page < 1) {
        now_page = 1; // 시작 페이지
      }
    }
    // -------------------------------------------------------------------------------------

    ra.addAttribute("cateno", cateno);
    ra.addAttribute("word", word);
    ra.addAttribute("now_page", now_page);
    
    return "redirect:/contents/list_by_cateno";    
    
  }   
  
 
}

