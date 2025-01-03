package dev.mvc.surveyitem;

import java.util.ArrayList;

public interface ItemProcInter {
  
  /**
   * 등록
   * @param itemVO
   * @return
   */
  public int create(ItemVO itemVO);
  
  /**
   * 조회
   * @param itemno
   * @return
   */
  public ItemVO read(int itemno);
  
  /**
   * 목록
   * @return
   */
  public ArrayList<ItemVO> list_all();
  
  /**
   * 수정
   * @param itemVO
   * @return
   */
  public int update(ItemVO itemVO);
  
  /**
   * 삭제
   * @param itemVO
   * @return
   */
  public int delete(int itemVO);

}
