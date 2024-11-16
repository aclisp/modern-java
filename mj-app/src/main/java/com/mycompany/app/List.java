package com.mycompany.app;

import static java.util.Objects.requireNonNull;

public class List {
    static ListNode first(ListNode list) {
        requireNonNull(list);
        return list;
    }

    static ListNode cons(ListNode first, ListNode rest) {
        requireNonNull(first);
        first.next = rest;
        return first;
    }

    static ListNode rest(ListNode list) {
        if (list == null) return null;
        return list.next;
    }

    static ListNode restN(int n, ListNode list) {
        while (n-- > 0) {
            list = rest(list);
        }
        return list;
    }

    static ListNode fromArray(int[] array) {
        if (array == null || array.length == 0) return null;

        ListNode res = new ListNode(array[array.length-1]);
        for (int i=array.length-2; i>=0; i--) {
            res = cons(new ListNode(array[i]), res);
        }
        return res;
    }

    static String toString(ListNode list) {
        StringBuilder res = new StringBuilder();
        while (list != null) {
            ListNode node = first(list);
            list = rest(list);
            res.append(node.val);
            res.append(",");
        }
        return res.toString();
    }

    static ListNode reverse(ListNode list) {
        ListNode res = null;
        while (list != null) {
            ListNode node = list;
            list = list.next;
            node.next = res;
            res = node;
        }
        return res;
    }

    static ListNode reverseBetween(ListNode head, int left, int right) {
        ListNode dummy = new ListNode(0); // created dummy node
        dummy.next = head;
        ListNode prev = dummy; // intialising prev pointer on dummy node

        for(int i = 0; i < left - 1; i++)
            prev = prev.next; // adjusting the prev pointer on it's actual index

        ListNode curr = prev.next; // curr pointer will be just after prev
        // reversing
        for(int i = 0; i < right - left; i++){
            ListNode forw = curr.next; // forw pointer will be after curr
            curr.next = forw.next;
            forw.next = prev.next;
            prev.next = forw;
        }
        return dummy.next;
    }

    public static void main(String[] args) {
    }
}

class ListNode {
    int val;
    ListNode next;

    ListNode() {
    }

    ListNode(int val) {
        this.val = val;
    }

    ListNode(int val, ListNode next) {
        this.val = val;
        this.next = next;
    }
}
