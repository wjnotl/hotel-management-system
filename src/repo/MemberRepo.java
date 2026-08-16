package repo;

import adt.ArrayList;
import adt.DoublyLinkedHashMap;
import adt.ListInterface;
import adt.MapInterface;
import entity.Member;
import util.BinaryFileUtil;

public class MemberRepo {
  private final BinaryFileUtil<ListInterface<Member>> fileUtil;
  private ListInterface<Member> memberList;

  // Bounded LRU Cache (Capacity: 50 active member entries)
  private final MapInterface<String, Member> memberLruCache =
      new DoublyLinkedHashMap<>(16, 0.75, 50, true);

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
    if (member == null) return;
    memberList.add(member);
    if (member.getMemberId() != null) {
      memberLruCache.put(member.getMemberId().toLowerCase(), member);
    }
    save();
  }

  public boolean updateMember(Member updatedMember) {
    if (updatedMember == null || memberList == null) return false;

    for (int i = 1; i <= memberList.getNumberOfEntries(); i++) {
      Member existing = memberList.getEntry(i);
      if (existing != null && existing.equals(updatedMember)) {
        memberList.replace(i, updatedMember);
        if (updatedMember.getMemberId() != null) {
          memberLruCache.put(updatedMember.getMemberId().toLowerCase(), updatedMember);
        }
        save();
        return true;
      }
    }
    return false;
  }

  public void addOrUpdateMember(Member member) {
    if (!updateMember(member)) {
      addMember(member);
    }
  }

  public Member findById(String memberId) {
    if (memberId == null || memberList == null) return null;

    // 1. O(1) Fast LRU Cache Hit
    Member cached = memberLruCache.get(memberId.toLowerCase());
    if (cached != null) {
      return cached;
    }

    // 2. Cache Miss: Scan list & populate LRU cache
    for (int i = 1; i <= memberList.getNumberOfEntries(); i++) {
      Member m = memberList.getEntry(i);
      if (m != null && memberId.equalsIgnoreCase(m.getMemberId())) {
        memberLruCache.put(memberId.toLowerCase(), m);
        return m;
      }
    }
    return null;
  }

  public ListInterface<Member> getMemberList() {
    return memberList;
  }
}
