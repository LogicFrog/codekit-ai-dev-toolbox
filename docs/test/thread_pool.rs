// thread_pool.rs
use std::collections::VecDeque;
use std::sync::{Arc, Mutex, Condvar};
use std::thread;

type Task = Box<dyn FnOnce() + Send + 'static>;

struct ThreadPool {
    workers: Vec<thread::JoinHandle<()>>,
    sender: Arc<(Mutex<VecDeque<Task>>, Condvar)>,
}

impl ThreadPool {
    fn new(size: usize) -> Self {
        assert!(size > 0);
        let sender = Arc::new((Mutex::new(VecDeque::new()), Condvar::new()));

        let workers: Vec<_> = (0..size)
            .map(|id| {
                let receiver = Arc::clone(&sender);
                thread::spawn(move || loop {
                    let (lock, cvar) = &*receiver;
                    let mut queue = lock.lock().unwrap();

                    while queue.is_empty() {
                        queue = cvar.wait(queue).unwrap();
                    }

                    if let Some(task) = queue.pop_front() {
                        drop(queue);
                        println!("Worker {id} executing task");
                        task();
                    }
                })
            })
            .collect();

        Self { workers, sender }
    }

    fn execute<F>(&self, f: F)
    where
        F: FnOnce() + Send + 'static,
    {
        let (lock, cvar) = &*self.sender;
        let mut queue = lock.lock().unwrap();
        queue.push_back(Box::new(f));
        cvar.notify_one();
    }
}
