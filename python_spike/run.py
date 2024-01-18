import logging

from caseprocessor import subscriber

if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    subscriber.subscribe()