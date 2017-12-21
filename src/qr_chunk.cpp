#include "../include/base_64.h"
#include "../include/qr_chunk.hpp"

#include <iostream>
#include <fstream>
#include <math.h>
#include <string.h>
#include <exception>


QrChunk::QrChunk(std::string data) {
  m_header = data.substr(0, m_header_size);
  m_body = data.substr(m_header_size, data.size() - m_header_size);
}

QrChunk::QrChunk(cv::Mat image) {
  m_image = image;
}

std::string QrChunk::getHeader() { return m_header; }

std::string QrChunk::getBody() { return m_body; }

std::string QrChunk::getData() { return m_header + m_body; }

int QrChunk::getCount () const {
  return this->count;
}

int QrChunk::getIndex () const {
  return this->index;
}

void QrChunk::setBody(std::string data) { m_body = data; }

void QrChunk::setImage(cv::Mat image) { m_image = image; }

bool QrChunk::operator<( const QrChunk& val ) const {
  return (this->getIndex() < val.getIndex());
}

bool has_only_digits(const std::string s){
  std::size_t found = s.find_first_not_of("0123456789");
  if (found == std::string::npos) {
    // std::cout << "not found" << std::endl;
    return true;
  } else {
    if (s[found] == '\0') {
      return true;
    }
    return false;
  }
}

bool QrChunk::setData ( std::string _message ) {

    if (_message.length() < m_header_size + 4 ) return false;

    m_header = _message.substr(0, m_header_size);
    m_body =  _message.substr(m_header_size, message.size() - m_header_size);

    std::string t_header = base64_decode(m_header).substr(0,2);
    std::string t_count = base64_decode(m_header).substr(2,4);

    if (!has_only_digits(t_header)) {return false;}
    if (!has_only_digits(t_count)) {return false;}

    this->index = std::stoi(t_header);
    this->count = std::stoi(t_count);
    this->message = _message;

    return true;

}
