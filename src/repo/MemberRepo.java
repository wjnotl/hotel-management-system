package repo;

import adt.ArrayList;
import adt.ListInterface;
import entity.Member;
import util.BinaryFileUtil;

public class MemberRepo {
  private final BinaryFileUtil<ListInterface<Member>> fileUtil;
  private ListInterface<Member> memberList;

  public MemberRepo() {
    this.fileUtil = new BinaryFileUtil<>("members.dat");
    load();
  }

  private void load() {
    this.memberList = fileUtil.retrieveFromFile();
    if (this.memberList == null) {
      this.memberList = new ArrayList<>();
    }
  }

  private void save() {
    fileUtil.saveToFile(memberList);
  }

  public void addMember(Member member) {
    memberList.add(member);
    save();
  }

  public Member findById(String memberId) {
    if (memberId == null) return null;
    for (int i = 1; i <= memberList.getNumberOfEntries(); i++) {
      Member m = memberList.getEntry(i);
      if (m != null && memberId.equalsIgnoreCase(m.getMemberId())) {
        return m;
      }
    }
    return null;
  }

  public ListInterface<Member> getMemberList() {
    return memberList;
  }
}
