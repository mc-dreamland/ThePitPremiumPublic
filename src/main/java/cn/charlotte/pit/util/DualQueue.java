package cn.charlotte.pit.util;

import java.util.NoSuchElementException;

public class DualQueue<E> {

    private Node<E> head;
    private Node<E> tail;
    private int size;

    public DualQueue() {
        head = null;
        tail = null;
        size = 0;
    }

    /**
     * 添加元素到队列尾部
     */
    public boolean offer(E element) {
        Node<E> newNode = new Node<>(element);

        if (tail == null)
            head = tail = newNode;
        else {
            tail.next = newNode;
            tail = newNode;
        }
        size++;
        return true;
    }

    /**
     * 添加元素到队列尾部（与offer相同）
     */
    public boolean add(E element) {
        return offer(element);
    }

    /**
     * 读取并删除队首元素（标准的poll操作）
     *
     * @return 队首元素，如果队列为空则返回null
     */
    public E poll() {
        if (isEmpty()) return null;

        E data = head.data;
        head = head.next;

        if (head == null) tail = null;

        size--;
        return data;
    }

    /**
     * 读取队首元素但不删除
     *
     * @return 队首元素，如果队列为空则返回null
     */
    public E peekPoll() {
        if (isEmpty()) return null;

        return head.data;
    }

    /**
     * 读取队首元素但不删除（与peekPoll相同）
     */
    public E peek() {
        return peekPoll();
    }

    /**
     * 读取并删除队首元素，如果队列为空则抛出异常
     */
    public E remove() {
        if (isEmpty()) throw new NoSuchElementException("Queue is empty");
        return poll();
    }

    /**
     * 读取队首元素但不删除，如果队列为空则抛出异常
     */
    public E element() {
        if (isEmpty()) throw new NoSuchElementException("Queue is empty");

        return peekPoll();
    }

    /**
     * 检查队列是否为空
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * 获取队列大小
     */
    public int size() {
        return size;
    }

    /**
     * 清空队列
     */
    public void clear() {
        head = null;
        tail = null;
        size = 0;
    }

    /**
     * 转换为数组
     */
    @SuppressWarnings("unchecked")
    public E[] toArray() {
        E[] array = (E[]) new Object[size];
        Node<E> current = head;
        for (int i = 0; i < size; i++) {
            array[i] = current.data;
            current = current.next;
        }
        return array;
    }

    /**
     * 判断队列是否包含指定元素
     */
    public boolean contains(Object o) {
        Node<E> current = head;
        while (current != null) {
            if (o == null) {
                if (current.data == null) {
                    return true;
                }
            } else {
                if (o.equals(current.data)) {
                    return true;
                }
            }
            current = current.next;
        }
        return false;
    }

    /**
     * 移除指定元素（第一次出现的位置）
     */
    public boolean remove(Object o) {
        if (isEmpty()) return false;

        if (o == null) {
            if (head.data == null) {
                poll();
                return true;
            }
        } else {
            if (o.equals(head.data)) {
                poll();
                return true;
            }
        }

        Node<E> current = head;
        Node<E> prev = null;

        while (current != null) {
            boolean match;
            if (o == null)
                match = current.data == null;
            else
                match = o.equals(current.data);

            if (match) {
                if (prev == null) poll();
                else {
                    prev.next = current.next;
                    if (current == tail) tail = prev;
                    size--;
                }
                return true;
            }
            prev = current;
            current = current.next;
        }

        return false;
    }

    /**
     * 获取指定索引位置的元素（不删除）
     *
     * @param index 索引位置，0表示队首
     * @return 指定位置的元素
     * @throws IndexOutOfBoundsException 如果索引越界
     */
    public E get(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException(String.format("Index: %d, Size: %d", index, size));

        Node<E> current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }

        return current.data;
    }

    /**
     * 覆盖指定索引位置的元素
     *
     * @param index   要覆盖的位置
     * @param element 新元素
     * @throws IndexOutOfBoundsException 如果索引越界
     */
    public void place(int index, E element) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException(String.format("Index: %d, Size: %d", index, size));

        Node<E> current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        current.data = element;
    }

    private static class Node<E> {

        E data;
        Node<E> next;

        Node(E data) {
            this.data = data;
            this.next = null;
        }
    }
}