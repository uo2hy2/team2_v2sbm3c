package dev.mvc.member;

import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;

@Setter @Getter
public class MemberVO {
    /** 회원 번호 */
    private int memberno;
    /** 등급 번호 */
    private Integer gradeno;
    /** 아이디(이메일) */
    private String id = "";
    /** 패스워드 */
    private String passwd = "";
    /** 이메일 주소 */
    private String email="";
    /** 회원 성명 */
    private String name = "";
    /** 회원 별명 */
    private String nickname = "";
    /** 생년월일 */
    private String birth = "";
    /** 우편 번호 */
    private String zipcode = "";
    /** 주소 1 */
    private String address1 = "";
    /** 주소 2 */
    private String address2 = "";
    /** 가입일 */
    private String mdate = "";
    /** 등급 */
    private int grade = 0;
    
    // -----------------------------------------------------------------------------------
    /** 파일 업로드 관련 */
    private MultipartFile file1MF = null;
    /** 메인 이미지 크기 단위,  파일 크기 */
    private String size1_label = "";
    /** 프로필 이미지 */
    private String pf_img = "";
    /** 실제 저장된 프로필 이미지 */
    private String file1saved = "";
    /** 프로필 이미지 preview */
    private String thumb1 = "";
    /** 메인 이미지 크기 */
    private long size1 = 0;
    // -----------------------------------------------------------------------------------
    
    /** 등록된 패스워드 */
    private String old_passwd = "";
    /** id 저장 여부 */
    private String id_save = "";
    /** passwd 저장 여부 */
    private String passwd_save = "";
    /** 이동할 주소 저장 */
    private String url_address = "";
}

