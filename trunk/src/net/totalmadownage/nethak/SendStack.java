package net.totalmadownage.nethak;

public class SendStack {
	
	private Object[] stackArray;
	private volatile int topOfStack;
	
	SendStack(int capacity) {
		stackArray = new Object[capacity];
		topOfStack = -1;
	}
	public synchronized Object pop() {
		while (isEmpty()) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		Object obj = stackArray[0];
		for(int x=0; x<topOfStack; x++)
		{
			stackArray[x] = stackArray[x+1];
		}
		stackArray[topOfStack--] = null;
		notify();
		return obj;
	}
	public synchronized void push(Object element) {
		while (isFull()) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		stackArray[++topOfStack] = element;
		notify();
	}
	public boolean isFull() {
		return topOfStack >= stackArray.length - 1;
	}
	public boolean isEmpty() {
		return topOfStack < 0;
	}

}
